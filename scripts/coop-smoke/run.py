#!/usr/bin/env python3
"""Launch one disposable dedicated dev server and two private Xvfb clients, never release test scaffolding."""
import argparse
import json
import os
from pathlib import Path
import socket
import subprocess
import time

REPO = Path(__file__).resolve().parents[2]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--timeout', type=int, default=900)
    options = parser.parse_args()
    subprocess.run([str(REPO / 'gradlew'), '--no-daemon', '-I', str(REPO / 'scripts/coop-smoke/prepare.gradle'),
                    ':fabric:prepareCoopSmoke'], cwd=REPO, check=True, timeout=600)
    launch = json.loads((REPO / 'run/coop-smoke/launch.json').read_text())
    root = REPO / 'run/coop-smoke' / time.strftime('%Y%m%d-%H%M%S')
    root.mkdir(parents=True, exist_ok=False)
    (root / 'screenshots').mkdir()
    with socket.socket() as listener:
        listener.bind(('127.0.0.1', 0))
        port = listener.getsockname()[1]
    server_dir = root / 'server'
    server_dir.mkdir()
    (server_dir / 'eula.txt').write_text('eula=true\n')
    (server_dir / 'server.properties').write_text(
        f'server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nmax-players=4\n'
        'level-type=minecraft:flat\ngenerator-settings={"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:dirt","height":2},{"block":"minecraft:grass_block","height":1}],"biome":"minecraft:plains"}\n'
        'gamemode=adventure\nspawn-protection=0\nview-distance=5\nsimulation-distance=5\n'
        'allow-flight=true\nenforce-secure-profile=false\n')
    processes = []
    logs = []

    def start(kind, directory, name=None):
        spec = launch[kind]
        jvm = [arg for arg in spec['jvm'] if not arg.startswith('-Dfabric-api.gametest')]
        args = [arg for arg in spec['args'] if arg != 'nogui']
        command = [spec['java'], '-Xmx3G' if kind == 'server' else '-Xmx1536M', '-Xms256M',
                   '-XX:ActiveProcessorCount=1', '-XX:+UseSerialGC', *jvm,
                   f'-Dgenshin.coopSmoke={root}', f'-Dgenshin.coopSmoke.port={port}']
        if name:
            command += [f'-Dgenshin.coopSmoke.name={name}', '-Dfabric.client.gametest=true']
        command += ['-cp', spec['classpath'], spec['main'], *args]
        if name:
            command += ['--gameDir', str(directory), '--username', name, '--offlineDeveloperMode',
                        '--width', '1280', '--height', '720']
            command = ['xvfb-run', '-a', '--server-args=-screen 0 1280x720x24 -nolisten tcp', *command]
        else:
            command += ['nogui']
        environment = dict(os.environ, ALSOFT_DRIVERS='null', LIBGL_ALWAYS_SOFTWARE='1',
                           DBUS_SESSION_BUS_ADDRESS='unix:path=/nonexistent/genshin-coop-smoke')
        # NixOS has no default Xcursor theme; GLFW's GUI cursors need an installed private-display theme.
        system_icons = Path('/run/current-system/sw/share/icons')
        if (system_icons / 'Breeze_Light' / 'cursors').is_dir():
            environment['XCURSOR_THEME'] = 'Breeze_Light'
            environment['XCURSOR_PATH'] = str(system_icons)
        environment.pop('DISPLAY', None)
        environment.pop('WAYLAND_DISPLAY', None)
        runtime = directory / 'xdg-runtime'
        runtime.mkdir(mode=0o700)
        environment['XDG_RUNTIME_DIR'] = str(runtime)
        log = (root / f'{name or "server"}.log').open('w')
        logs.append(log)
        process = subprocess.Popen(command, cwd=directory, env=environment, stdout=log, stderr=subprocess.STDOUT,
                                   start_new_session=True)
        processes.append(process)
        return process

    try:
        server = start('server', server_dir)
        deadline = time.monotonic() + options.timeout
        while True:
            if server.poll() is not None:
                raise RuntimeError(f'Server exited before listening; see {root}/server.log')
            try:
                with socket.create_connection(('127.0.0.1', port), timeout=1):
                    break
            except OSError:
                if time.monotonic() >= deadline:
                    raise TimeoutError('Server did not listen before deadline')
                time.sleep(1)
        displays = set()
        for name in ('CoopHost', 'CoopGuest'):
            directory = root / name
            directory.mkdir()
            (directory / 'options.txt').write_text('renderDistance:5\nsimulationDistance:5\nmaxFps:30\n')
            client = start('client', directory, name)
            receipt = root / f'{name}-display'
            # Installed xvfb-run -a allocates before starting Xvfb; parallel starts can race for :99.
            while not receipt.is_file() or receipt.stat().st_size == 0:
                if client.poll() is not None:
                    raise RuntimeError(f'{name} exited before display handshake; see {root}/{name}.log')
                if time.monotonic() >= deadline:
                    raise TimeoutError(f'{name} did not initialize its private display')
                time.sleep(.25)
            display = receipt.read_text().strip()
            if not display.startswith(':') or display in displays:
                raise AssertionError(f'Clients must use distinct private Xvfb displays: {display}')
            displays.add(display)
        while any(process.poll() is None for process in processes):
            if (root / 'failure.txt').exists():
                raise RuntimeError((root / 'failure.txt').read_text())
            if time.monotonic() >= deadline:
                raise TimeoutError(f'Two-client smoke timed out; logs at {root}')
            if any(process.poll() not in (None, 0) for process in processes):
                raise RuntimeError(f'A smoke process failed; logs at {root}')
            time.sleep(1)
        report = json.loads((root / 'server-result.json').read_text())
        if not report['passed'] or any(process.returncode != 0 for process in processes):
            raise AssertionError('Smoke process/server assertions failed')
        for name in ('coop-host-sees-lisa.png', 'coop-guest-sees-kaeya.png'):
            if not (root / 'screenshots' / name).is_file():
                raise AssertionError(f'Missing screenshot {name}')
        print(json.dumps(dict(report, artifacts=str(root), command='python3 scripts/coop-smoke/run.py'), indent=2))
    finally:
        import signal
        for process in processes:
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
        for process in processes:
            try:
                process.wait(timeout=30)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)
                process.wait()
        for log in logs:
            log.close()


if __name__ == '__main__':
    main()
