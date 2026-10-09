"""Receipt/vanilla-asset analysis; signal levels are evidence, not a claim somebody listened."""
import array
from collections import Counter, defaultdict
import json
import math
from pathlib import Path
import re
import statistics
import subprocess


def read_events(root):
    events = []
    volumes = {}
    for line in (root / 'events.tsv').read_text().splitlines():
        epoch, kind, cue, text = line.split('\t', 3)
        event = dict(epoch=float(epoch)/1000, kind=kind, cue=cue, text=text)
        if kind == 'VOLUME':
            source, volume = text.split('=', 1)
            volumes[source] = float(volume)
        if kind == 'SOUND':
            event['id'] = text.split()[0]
            for key, value in re.findall(r'(\w+)=([^ ]+)', text):
                event[key] = float(value) if key in ('volume', 'pitch') else int(value) if key == 'tick' else value
            event['category_gain'] = volumes.get('MASTER', 1.) * (1. if event.get('source') == 'MASTER' else volumes.get(event.get('source'), 1.))
        events.append(event)
    return events


class Assets:
    def __init__(self, root, index):
        self.root = Path(root)
        self.index = json.loads((self.root / 'indexes' / (index + '.json')).read_text())['objects']
        self.sounds = json.loads(self.path('minecraft/sounds.json').read_text())
        self.levels = {}

    def path(self, name):
        digest = self.index[name]['hash']
        return self.root / 'objects' / digest[:2] / digest

    def level(self, name):
        name = name.removeprefix('minecraft:')
        if name not in self.levels:
            raw = subprocess.check_output(['ffmpeg', '-v', 'error', '-i', str(self.path('minecraft/sounds/' + name + '.ogg')),
                                           '-ac', '1', '-ar', '48000', '-f', 'f32le', '-'], timeout=60)
            samples = array.array('f'); samples.frombytes(raw)
            # 10-ms energy buckets, sliding 100-ms RMS. No normalization or category/distance gain.
            energy = [sum(x*x for x in samples[i:i+480]) for i in range(0, len(samples), 480)]
            energy.extend([0.] * max(0, 10-len(energy)))
            window = sum(energy[:10]); maximum = window
            for i in range(10, len(energy)):
                window += energy[i] - energy[i-10]; maximum = max(maximum, window)
            self.levels[name] = 10*math.log10(max(maximum/4800, 1e-20))
        return self.levels[name]

    def files(self, event, volume=1., pitch=1.):
        if event not in self.sounds:
            raise AssertionError('Missing 26.2 event: ' + event)
        for item in self.sounds[event]['sounds']:
            item = {'name': item} if isinstance(item, str) else item
            resolved_volume = volume * item.get('volume', 1.)
            resolved_pitch = pitch * item.get('pitch', 1.)
            if item.get('type') == 'event':
                yield from self.files(item['name'].removeprefix('minecraft:'), resolved_volume, resolved_pitch)
            else:
                yield dict(file=item['name'], resolved_volume=resolved_volume, resolved_pitch=resolved_pitch,
                           rendered_db=self.level(item['name']) + 20*math.log10(min(1., resolved_volume)))

    def validate_table(self, repo):
        source = (repo / 'common/src/main/java/io/github/brainage04/genshininminecraft/rules/CombatAudio.java').read_text()
        result = {}
        for cue, event, volume, pitch in re.findall(r'(\w+)\("([^"]+)", ([.\d]+)F, ([.\d]+)F', source):
            files = list(self.files(event, float(volume), float(pitch)))
            if not files or min(f['rendered_db'] for f in files) < -45:
                raise AssertionError('Inaudible cue candidate: ' + cue + ' ' + str(files))
            result[cue] = dict(event='minecraft:' + event, volume=float(volume), pitch=float(pitch), files=files,
                               min_db=min(f['rendered_db'] for f in files), max_db=max(f['rendered_db'] for f in files))
        return result


def analyze(root, assets, origin, recording, destination=None):
    events = read_events(root)
    receipts = [e for e in events if e['kind'] == 'SOUND']
    by_id = defaultdict(list)
    for event in receipts:
        event['time'] = event['epoch'] - origin
        event['asset_db'] = assets.level(event['file'])
        event['resolved_asset_db'] = event['asset_db'] + 20*math.log10(max(1e-10, min(1., event['volume'])))
        event['rendered_db'] = event['resolved_asset_db'] + 20*math.log10(event['category_gain']) if event['category_gain'] > 0 else None
        if event['rendered_db'] is not None:
            by_id[event['id']].append(event['rendered_db'])
    levels = {key: dict(count=len(values), median_db=statistics.median(values), min_db=min(values), max_db=max(values))
              for key, values in sorted(by_id.items())}
    busy_start = next(e['epoch'] for e in events if e['kind'] == 'CUE' and e['cue'].startswith('Busy fight START'))
    busy = [e for e in receipts if busy_start <= e['epoch'] < busy_start+20]
    counts = Counter(e['id'] for e in busy)
    duplicates = Counter()
    duplicate_groups = Counter()
    in_duplicate = {}
    last = {}
    for e in busy:
        key = e['id']
        stamp = e.get('tick')
        if stamp is not None:
            same = last.get(key) == stamp
        else:
            stamp = e['epoch']
            same = key in last and stamp-last[key] <= .05
        if same:
            duplicates[key] += 1
            if not in_duplicate.get(key, False):
                duplicate_groups[key] += 1
            in_duplicate[key] = True
        else:
            in_duplicate[key] = False
        last[key] = stamp
    rejects = [e for e in busy if e['id'] == 'minecraft:ui.button.click' and abs(e['volume']-.25)<.001 and abs(e['pitch']-.7)<.01]
    result = dict(recording=str(recording), timestamp_origin_epoch_seconds=origin,
                  method='Selected vanilla asset loudest 100 ms RMS (10 ms steps) + 20 log10(min(1, resolved listener volume)) + logged category/master gain (historical default 1); pitch logged, not resampled; ignores overlap and distance. Muted receipts have null rendered level and are excluded from levels. Nobody listened.',
                  receipts=receipts, levels=levels, muted_receipts=dict(Counter(e['id'] for e in receipts if e['rendered_db'] is None)),
                  busy=dict(start=busy_start-origin, duration=20, counts=dict(counts), rejection_clicks=len(rejects),
                            same_tick_duplicates=dict(duplicates), same_tick_duplicate_groups=dict(duplicate_groups),
                            duplicate_method='exact client world tick' if all('tick' in e for e in busy) else 'same ID within 50 ms (historical receipt proxy)'))
    if destination:
        destination.write_text(json.dumps(result, indent=2)+'\n')
    return result, events


def cue_sheet(root, report, events):
    cues = [e for e in events if e['kind'] == 'CUE']
    origin = report['timestamp_origin_epoch_seconds']
    lines = ['# Repeatable vanilla sound review', '',
             '- Private Xvfb and hardware-free PipeWire/Pulse null sink, all processes in a 12 GiB / 3 CPU scope.',
             '- Master/game categories 100%; MUSIC/RECORDS 0%. No normalization. Nobody listened; rendered levels are signal analysis.',
             '- Asset method, selected files, resolved volume/pitch and exact world ticks: `report.json` / `events.tsv`.',
             '- Muted SoundEngine receipts remain in TSV/JSON but do not count as audible events or rendered levels.',
             '- Hydro hit and Shatter have no reachable starter-party path; their assets are validated separately.', '',
             '| Time (s) | Action | Unmuted resolved event IDs / plays |', '| --- | --- | --- |']
    for i, cue in enumerate(cues):
        end = cues[i+1]['epoch'] if i+1 < len(cues) else float('inf')
        counts = Counter(e['id'] for e in report['receipts'] if cue['epoch'] <= e['epoch'] < end and e['rendered_db'] is not None)
        lines.append(f"| {cue['epoch']-origin:.3f} | {cue['cue']} | " + ', '.join(f'`{event}` ×{n}' for event, n in sorted(counts.items())) + ' |')
    lines += ['', '## Per-event rendered levels', '', '| Event | Plays | Median / minimum / maximum dBFS |', '| --- | --- | --- |']
    for event, values in report['levels'].items():
        lines.append(f"| `{event}` | {values['count']} | {values['median_db']:.1f} / {values['min_db']:.1f} / {values['max_db']:.1f} |")
    (root/'cues.md').write_text('\n'.join(lines)+'\n')
