#!/usr/bin/env python3
"""Record the real SoundReview client sequence with private display/audio, then measure vanilla asset levels."""
import argparse
import json
import os
from pathlib import Path
import select
import signal
import shutil
import re
import subprocess
import sys
import tempfile
import time

from analyze import Assets, analyze, cue_sheet

REPO = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--timeout', type=int, default=1800)
    parser.add_argument('--baseline', type=Path, default=REPO/'run/sound-review/20261009-234316')
    parser.add_argument('--analyze', type=Path, help='Reanalyze a completed capture without launching clients')
    parser.add_argument('--ffmpeg', default=os.environ.get('FFMPEG', 'ffmpeg'), help='Capture build with x11grab and pulse input support')
    parser.add_argument('--in-scope', action='store_true', help=argparse.SUPPRESS)
    options = parser.parse_args()
    if not options.in_scope:
        os.execvp('systemd-run', ['systemd-run', '--user', '--scope', '--quiet', '-p', 'MemoryMax=12G', '-p', 'CPUQuota=300%',
                                 sys.executable, str(Path(__file__).resolve()), *sys.argv[1:], '--in-scope'])
    if options.analyze:
        root = options.analyze.resolve()
        capture = json.loads((root/'capture.json').read_text())
        assets = Assets(capture['assets'], capture['asset_index'])
        finish(root, assets, capture, options.baseline)
        return
    ffmpeg = options.ffmpeg
    def capture_devices(binary):
        return subprocess.check_output([binary, '-hide_banner', '-devices'], stderr=subprocess.STDOUT, text=True, timeout=30)
    devices = capture_devices(ffmpeg)
    if ('x11grab' not in devices or 'pulse' not in devices) and ffmpeg == 'ffmpeg' and shutil.which('nix'):
        packages = subprocess.check_output(['nix', 'build', '--no-link', '--print-out-paths', 'nixpkgs#ffmpeg-full'], text=True, timeout=600).splitlines()
        ffmpeg = str(next(Path(package)/'bin/ffmpeg' for package in packages if (Path(package)/'bin/ffmpeg').is_file()))
        devices = capture_devices(ffmpeg)
    if 'x11grab' not in devices or 'pulse' not in devices:
        raise RuntimeError('Capture requires ffmpeg with x11grab and Pulse inputs; pass --ffmpeg <full-build>')
    subprocess.run([str(REPO/'gradlew'), '--no-daemon', '-I', str(REPO/'scripts/sound-review/prepare.gradle'),
                    ':fabric:prepareSoundReview'], cwd=REPO, check=True, timeout=600)
    launch = json.loads((REPO/'run/sound-review/launch.json').read_text())
    cfg = (REPO/'.gradle/loom-cache/projects/fabric/launch.cfg').read_text().splitlines()
    asset_root = cfg[cfg.index('\t--assetsDir')+1].strip()
    asset_index = cfg[cfg.index('\t--assetIndex')+1].strip()
    assets = Assets(asset_root, asset_index)
    validated = assets.validate_table(REPO)
    root = REPO/'run/sound-review'/time.strftime('%Y%m%d-%H%M%S')
    root.mkdir(parents=True, exist_ok=False)
    (root/'validated-candidates.json').write_text(json.dumps(validated, indent=2)+'\n')
    game = root/'game'; game.mkdir()
    (game/'eula.txt').write_text('eula=true\n')
    (game/'options.txt').write_text('renderDistance:5\nsimulationDistance:5\nmaxFps:30\nguiScale:3\nsoundCategory_music:0.0\nsoundCategory_record:0.0\n')
    processes = []; logs = []
    deadline = time.monotonic()+options.timeout

    def start(name, command, environment, **kwargs):
        log = (root/(name+'.log')).open('w'); logs.append(log)
        proc = subprocess.Popen(command, cwd=game, env=environment, stdout=log, stderr=subprocess.STDOUT,
                                start_new_session=True, **kwargs)
        processes.append(proc)
        return proc

    def await_condition(condition, description):
        while not condition():
            if time.monotonic() >= deadline: raise TimeoutError(description)
            for proc in processes:
                if proc.poll() is not None: raise RuntimeError(f'Process exited {proc.returncode} while {description}; see {root}')
            time.sleep(.1)

    with tempfile.TemporaryDirectory(prefix='genshin-sound-') as private:
        runtime = Path(private); runtime.chmod(0o700)
        config = root/'config'; (config/'pipewire').mkdir(parents=True)
        state = root/'state'; state.mkdir()
        environment = dict(os.environ, XDG_RUNTIME_DIR=str(runtime), XDG_CONFIG_HOME=str(config), XDG_STATE_HOME=str(state),
                           PIPEWIRE_RUNTIME_DIR=str(runtime), PIPEWIRE_REMOTE='pipewire-0',
                           PULSE_SERVER='unix:'+str(runtime/'pulse/native'), PULSE_COOKIE=str(runtime/'pulse-cookie'),
                           DBUS_SESSION_BUS_ADDRESS='unix:path=/nonexistent/genshin-sound-review',
                           ALSOFT_DRIVERS='pulse', LIBGL_ALWAYS_SOFTWARE='1', LP_NUM_THREADS='2', GENSHIN_SOUND_REVIEW=str(root))
        for key in ('DISPLAY', 'WAYLAND_DISPLAY', 'PIPEWIRE_CONFIG_DIR', 'PIPEWIRE_CONFIG_NAME', 'ALSOFT_CONF'):
            environment.pop(key, None)
        icons = Path('/run/current-system/sw/share/icons')
        if (icons/'Breeze_Light/cursors').is_dir():
            environment.update(XCURSOR_THEME='Breeze_Light', XCURSOR_PATH=str(icons))
        (config/'pipewire/pipewire.conf').write_text('''context.properties = { core.daemon = true core.name = pipewire-0 support.dbus = false default.clock.rate = 48000 }
context.spa-libs = { audio.convert.* = audioconvert/libspa-audioconvert support.* = support/libspa-support }
context.modules = [
 { name = libpipewire-module-protocol-native }
 { name = libpipewire-module-metadata }
 { name = libpipewire-module-spa-node-factory }
 { name = libpipewire-module-client-node }
 { name = libpipewire-module-client-device }
 { name = libpipewire-module-access args = { access.force = unrestricted } }
 { name = libpipewire-module-adapter }
 { name = libpipewire-module-link-factory }
 { name = libpipewire-module-session-manager }
]
context.objects = [ { factory = metadata args = { metadata.name = default } } ]
''')
        try:
            read_fd, write_fd = os.pipe()
            try:
                start('xvfb', ['Xvfb', '-displayfd', str(write_fd), '-screen', '0', '1280x720x24', '-nolisten', 'tcp'], environment,
                      pass_fds=(write_fd,))
                os.close(write_fd); write_fd = None
                if not select.select([read_fd], [], [], 30)[0]: raise TimeoutError('Xvfb private display')
                display = ':'+os.read(read_fd, 64).decode().strip()
                if not display[1:].isdigit(): raise RuntimeError('Invalid private Xvfb display')
            finally:
                os.close(read_fd)
                if write_fd is not None: os.close(write_fd)
            environment['DISPLAY'] = display
            (root/'private-display.txt').write_text(display+'\n')
            start('pipewire', ['pipewire', '-c', str(config/'pipewire/pipewire.conf')], environment)
            await_condition(lambda: (runtime/'pipewire-0').exists(), 'starting private PipeWire')
            start('pulse', ['pipewire-pulse'], environment)
            start('policy', ['wireplumber', '-p', 'policy'], environment)
            await_condition(lambda: (runtime/'pulse/native').exists(), 'starting private Pulse')
            def pactl(*args):
                return subprocess.check_output(['pactl', *args], env=environment, text=True, timeout=30)
            pactl('load-module', 'module-null-sink', 'sink_name=genshin_review', 'rate=48000', 'channels=2')
            pactl('set-default-sink', 'genshin_review')
            pactl('set-default-source', 'genshin_review.monitor')
            pactl('set-sink-volume', 'genshin_review', '100%')
            (root/'private-audio.txt').write_text(pactl('info'))
            jvm = [arg for arg in launch['jvm'] if not arg.startswith('-Dfabric-api.gametest')]
            client = start('client', [launch['java'], '-Xmx3G', '-Xms256M', '-XX:ActiveProcessorCount=2', '-XX:+UseSerialGC', *jvm,
                                     '-Dfabric.client.gametest=true', '-cp', launch['classpath'], launch['main'], *launch['args'],
                                     '--gameDir', str(game), '--username', 'SoundReview', '--offlineDeveloperMode',
                                     '--width', '1280', '--height', '720'], environment)
            await_condition(lambda: (root/'ready').exists(), 'waiting for real client/world readiness')
            origin = time.time()
            recorder = start('capture', [ffmpeg, '-y', '-nostdin', '-v', 'warning', '-copyts', '-thread_queue_size', '1024',
                              '-f', 'x11grab', '-framerate', '30', '-video_size', '1280x720', '-i', display,
                              '-thread_queue_size', '1024', '-f', 'pulse', '-sample_rate', '48000', '-i', 'genshin_review.monitor',
                              '-af', 'aresample=async=1000:min_hard_comp=0.01', '-c:v', 'libx264', '-preset', 'ultrafast', '-crf', '23',
                              '-threads', '2', '-pix_fmt', 'yuv420p', '-c:a', 'aac', '-b:a', '192k', str(root/'sound-review-raw.mkv')], environment)
            await_condition(lambda: (root/'sound-review-raw.mkv').exists(), 'starting private capture')
            time.sleep(.5)
            capture = dict(origin=origin, assets=asset_root, asset_index=asset_index, display=display,
                           recording=str(root/'sound-review.mp4'), resource_cap=dict(memory='12G', cpu='300%'))
            (root/'capture.json').write_text(json.dumps(capture, indent=2)+'\n')
            (root/'capture-started').write_text('ok\n')
            while client.poll() is None:
                if time.monotonic() >= deadline: raise TimeoutError('SoundReview sequence deadline')
                for proc in processes:
                    if proc is not client and proc.poll() is not None: raise RuntimeError('Capture dependency exited')
                time.sleep(.5)
            if client.returncode != 0 or not (root/'completed').exists(): raise AssertionError('SoundReview client did not complete')
            recorder.send_signal(signal.SIGINT)
            if recorder.wait(timeout=60) not in (0, 255): raise RuntimeError('ffmpeg capture failed')
            raw_probe = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-show_format', '-show_streams', '-of', 'json',
                                                            str(root/'sound-review-raw.mkv')], timeout=60))
            starts = {stream['codec_type']: float(stream['start_time']) for stream in raw_probe['streams']}
            if abs(starts['video'] - origin) > 10 or abs(starts['audio'] - starts['video']) > 2:
                raise AssertionError('Capture sources must share the wall-clock epoch: ' + str(starts))
            capture['origin'] = float(raw_probe['format']['start_time'])
            capture['raw_stream_starts'] = starts
            capture['origin_method'] = 'ffprobe raw copyts wall-clock start; one-input start_at_zero remux shifts both tracks together'
            subprocess.run([ffmpeg, '-v', 'error', '-y', '-copyts', '-start_at_zero', '-i', str(root/'sound-review-raw.mkv'),
                            '-c', 'copy', str(root/'sound-review.mp4')], check=True, timeout=120)
            (root/'capture.json').write_text(json.dumps(capture, indent=2)+'\n')
        finally:
            for proc in reversed(processes):
                if proc.poll() is None: os.killpg(proc.pid, signal.SIGTERM)
            for proc in reversed(processes):
                try: proc.wait(timeout=30)
                except subprocess.TimeoutExpired:
                    os.killpg(proc.pid, signal.SIGKILL); proc.wait()
            for log in logs: log.close()
    finish(root, assets, capture, options.baseline)


def finish(root, assets, capture, baseline):
    report, events = analyze(root, assets, capture['origin'], capture['recording'])
    report['validated_mapping'] = assets.validate_table(REPO)
    probe = subprocess.check_output(['ffprobe', '-v', 'error', '-show_streams', '-show_format', '-of', 'json', capture['recording']], timeout=60)
    (root/'ffprobe.json').write_bytes(probe)
    report['duration'] = float(json.loads(probe)['format']['duration'])
    def recorded_levels(start=None, duration=None):
        command = ['ffmpeg', '-hide_banner', '-nostdin']
        if start is not None: command += ['-ss', str(start)]
        command += ['-i', capture['recording']]
        if duration is not None: command += ['-t', str(duration)]
        command += ['-vn', '-af', 'aresample=async=1,volumedetect', '-f', 'null', '-']
        measured = subprocess.run(command, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE, text=True, check=True, timeout=120).stderr
        return {key: float(value) for key, value in re.findall(r'(mean_volume|max_volume):\s*(-?[\d.]+) dB', measured)}
    report['recorded_audio'] = recorded_levels()
    report['busy']['recorded_audio'] = recorded_levels(report['busy']['start'], 20)
    if report['recorded_audio'].get('max_volume', -100) <= -60:
        raise AssertionError('Sound review capture is effectively silent')
    packets = json.loads(subprocess.check_output(['ffprobe', '-v', 'error', '-select_streams', 'a', '-show_packets',
                                                 '-show_entries', 'packet=pts_time,duration_time', '-of', 'json', capture['recording']], timeout=60))['packets']
    gaps = [float(b['pts_time'])-float(a['pts_time'])-float(a['duration_time']) for a, b in zip(packets, packets[1:])]
    report['audio_packet_gaps'] = dict(count=sum(g > .001 for g in gaps), total_seconds=sum(g for g in gaps if g > .001),
                                       max_seconds=max(gaps, default=0))
    if baseline and baseline.is_dir():
        old = json.loads((baseline/'report.json').read_text())
        before, _ = analyze(baseline, assets, old['timestamp_origin_epoch_seconds'], old['recording'], root/'before-report.json')
        report['before_busy'] = before['busy']
        rows = ['# Before / after rendered level', '',
                'Per event: median / minimum / maximum dBFS of selected vanilla files at resolved receipt volumes and logged category/master gain. Muted receipts are excluded. Signal analysis, not listening.', '',
                '| Event ID | Before dBFS (plays) | After dBFS (plays) |', '| --- | --- | --- |']
        for event in sorted(set(before['levels']) | set(report['levels'])):
            def cell(values):
                return '—' if values is None else f"{values['median_db']:.1f} / {values['min_db']:.1f} / {values['max_db']:.1f} ({values['count']})"
            rows.append(f"| `{event}` | {cell(before['levels'].get(event))} | {cell(report['levels'].get(event))} |")
        rows += ['', '## Busy fight (20 s)', '', '| Count | Before | After |', '| --- | --- | --- |']
        for label, a, b in [('Piglin hurt plays', before['busy']['counts'].get('minecraft:entity.piglin.hurt', 0), report['busy']['counts'].get('minecraft:entity.piglin.hurt', 0)),
                            ('Rejected clicks', before['busy']['rejection_clicks'], report['busy']['rejection_clicks']),
                            ('Same-ID same-tick extra plays (historical ≤50 ms proxy)', sum(before['busy']['same_tick_duplicates'].values()), sum(report['busy']['same_tick_duplicates'].values())),
                            ('Piglin hurt duplicate extra plays', before['busy']['same_tick_duplicates'].get('minecraft:entity.piglin.hurt', 0), report['busy']['same_tick_duplicates'].get('minecraft:entity.piglin.hurt', 0)),
                            ('Piglin hurt adjacent-receipt duplicate groups', before['busy']['same_tick_duplicate_groups'].get('minecraft:entity.piglin.hurt', 0), report['busy']['same_tick_duplicate_groups'].get('minecraft:entity.piglin.hurt', 0))]:
            rows.append(f'| {label} | {a} | {b} |')
        (root/'before-after.md').write_text('\n'.join(rows)+'\n')
    (root/'report.json').write_text(json.dumps(report, indent=2)+'\n')
    cue_sheet(root, report, events)
    print(json.dumps(dict(artifacts=str(root), duration=report['duration'], busy=report['busy']), indent=2))


if __name__ == '__main__':
    main()
