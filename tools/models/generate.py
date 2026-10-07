#!/usr/bin/env python3
"""Original blocky character art. Python stdlib only; --check never writes files.

16 model units/block, foot origin, north-facing Bedrock skeleton. No imported meshes,
textures, traced pixels or game audio. Public character pages are colour/silhouette
references only. All coordinates, painted pixels and key poses below are authored here.
"""
import argparse
import json
import math
import re
from pathlib import Path
import struct
import zlib

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / 'common/src/main/resources/assets/genshininminecraft'
NAMES = ('aether', 'amber', 'kaeya', 'lisa')
FACES = ('north', 'south', 'east', 'west', 'up', 'down')
PHASES = {'idle': (120, True), 'walk': (48, True), 'run': (30, True), 'dash': (18, False),
          'jump': (24, False), 'fall': (60, True), 'land_soft': (15, False), 'land_hard': (30, False),
          'climb_idle': (120, True), 'climb_up': (60, True), 'climb_down': (60, True),
          'climb_left': (60, True), 'climb_right': (60, True), 'climb_jump': (18, False),
          'mantle': (24, False), 'glide_start': (18, False), 'glide_loop': (90, True),
          'glide_stop': (12, False), 'swim': (60, True)}
PALETTES = {
    'aether': {'skin': 'f3c9a0', 'hair': 'e7bc4f', 'cloth': 'f4eee3', 'dark': '493329', 'accent': 'cfa849', 'eye': 'bf8537'},
    'amber': {'skin': 'f1c4a3', 'hair': '634027', 'cloth': 'b52832', 'dark': '3e2925', 'accent': 'e6bb76', 'eye': 'ba7730'},
    'kaeya': {'skin': 'b87d5f', 'hair': '233f8a', 'cloth': 'e5e9ed', 'dark': '15294a', 'accent': '367cbc', 'eye': '75bbd5'},
    'lisa': {'skin': 'f1c8ab', 'hair': '724527', 'cloth': '754a95', 'dark': '35243f', 'accent': 'd5b666', 'eye': '78a34c'}}


def png(pixels, width=128, height=128):
    def chunk(kind, data):
        return struct.pack('>I', len(data)) + kind + data + struct.pack('>I', zlib.crc32(kind + data) & 0xffffffff)
    rows = b''.join(b'\x00' + bytes(pixels[y * width * 4:(y + 1) * width * 4]) for y in range(height))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('>IIBBBBB', width, height, 8, 6, 0, 0, 0)) + chunk(b'IDAT', zlib.compress(rows, 9)) + chunk(b'IEND', b'')


def rgb(value):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


class Model:
    def __init__(self, name):
        self.name = name
        self.palette = PALETTES[name] | {'white': 'f7f1df', 'metal': 'a7c4d5', 'black': '1b1820', 'wing': '79b5a6', 'rose': 'cf85ae'}
        self.pixels = bytearray(128 * 128 * 4)
        self.uvs = {}
        for index, (material, color) in enumerate(self.palette.items()):
            faces = {}
            for face, shade in zip(FACES, (1.0, .83, .9, .75, 1.1, .65)):
                tile = index * 6 + FACES.index(face)
                u, v = (tile % 12) * 10, (tile // 12) * 10
                faces[face] = {'uv': [u, v], 'uv_size': [8, 8]}
                for y in range(8):
                    for x in range(8):
                        # Directional face shading, seams and woven/highlight pixel bands.
                        factor = shade * (.90 if x in (0, 7) or y == 7 else 1.04 if y < 2 else 1)
                        factor *= 1 + (((x * 3 + y * 5 + index) % 7) - 3) * .008
                        self.pixel(u + x, v + y, tuple(min(255, round(c * factor)) for c in rgb(color)))
            self.uvs[material] = faces
        self.bones = []
        self.add('root', None, [0, 0, 0])
        self.add('hips', 'root', [0, 13, 0])
        self.add('torso', 'hips', [0, 14, 0])
        self.cube('hips', [-2.5, 12, -1.55], [5, 2.7, 3.1], 'dark')
        self.cube('torso', [-2.8, 14.3, -1.6], [5.6, 8.3, 3.2], 'cloth')
        self.cube('torso', [-2.9, 14.2, -1.7], [5.8, .7, 3.4], 'accent')
        self.cube('torso', [-.35, 15, -1.73], [.7, 6.5, .25], 'accent')
        self.cube('torso', [-1.05, 20.8, -1.82], [2.1, 1.2, .3], 'dark')
        self.add('head', 'torso', [0, 23, 0])
        self.cube('head', [-3.15, 23, -2.45], [6.3, 5.9, 4.9], 'skin')
        # The face has an exclusive larger patch, not a skin pasted onto vanilla geometry.
        u, v = 0, 110
        for y in range(16):
            for x in range(20):
                f = 1.04 - .012 * abs(x - 9.5) - .008 * y
                self.pixel(u + x, v + y, tuple(round(c * f) for c in rgb(self.palette['skin'])))
        for x0 in (4, 13):
            self.rect(u + x0, v + 6, 3, 2, rgb('fff9ee'))
            self.rect(u + x0 + 1, v + 6, 2, 3, rgb(self.palette['eye']))
            self.rect(u + x0, v + 5, 4, 1, rgb(self.palette['hair']))
            self.pixel(u + x0 + 1, v + 6, (255, 255, 244))
        self.rect(u + 8, v + 12, 4, 1, rgb('b77970'))
        self.bones[-1]['cubes'][-1]['uv']['north'] = {'uv': [u, v], 'uv_size': [20, 16]}
        self.add('hair', 'head', [0, 28, 0])
        self.cube('hair', [-3.35, 28, -2.6], [6.7, 1.25, 5.3], 'hair')
        self.cube('hair', [-3.35, 24.5, 1.8], [6.7, 4, 1], 'hair')
        for side in (-1, 1):
            self.cube('hair', [side * 2.8 - .45, 24, -1.8], [.9, 4.5, 2.7], 'hair')
        for i in range(4):
            self.cube('hair', [-3.05 + i * 1.6, 27.5 - (i % 2) * .4, -2.72], [1.65, 1.8, .8], 'hair', rotation=[0, 0, -8 + 6 * i])
        for side, sign in (('right', -1), ('left', 1)):
            x = sign * 3.8
            self.add(side + '_arm', 'torso', [x, 22, 0])
            self.cube(side + '_arm', [x - 1.15, 17.2, -1.15], [2.3, 5.3, 2.3], 'cloth')
            self.cube(side + '_arm', [x - 1.25, 21.3, -1.25], [2.5, 1.1, 2.5], 'accent')
            self.add(side + '_forearm', side + '_arm', [x, 17.5, 0])
            self.cube(side + '_forearm', [x - .95, 13, -.95], [1.9, 4.5, 1.9], 'skin')
            self.cube(side + '_forearm', [x - 1, 13, -1], [2, 1.8, 2], 'dark')
            self.add(side + '_hand', side + '_forearm', [x, 13, 0])
            self.cube(side + '_hand', [x - 1, 11.8, -1], [2, 1.5, 2], 'skin')
            x = sign * 1.55
            self.add(side + '_leg', 'hips', [x, 12.5, 0])
            self.cube(side + '_leg', [x - 1.2, 6.6, -1.25], [2.4, 6, 2.5], 'dark' if name != 'lisa' else 'skin')
            self.add(side + '_shin', side + '_leg', [x, 6.7, 0])
            self.cube(side + '_shin', [x - 1.1, 1, -1.2], [2.2, 5.8, 2.4], 'dark')
            self.cube(side + '_shin', [x - 1.17, 5.3, -1.28], [2.34, .8, 2.56], 'accent')
            self.add(side + '_foot', side + '_shin', [x, 1.2, 0])
            self.cube(side + '_foot', [x - 1.2, 0, -2], [2.4, 1.3, 3.1], 'dark')
        self.add('cape', 'torso', [0, 22, 1.75])
        self.add('accessory', 'head', [0, 28.5, 0])
        self.add('weapon', 'right_hand' if name in ('aether', 'kaeya') else 'left_hand', [-3.8 if name in ('aether', 'kaeya') else 3.8, 12.5, 0])
        self.add('glider_left', 'torso', [1, 21, 2])
        self.add('glider_right', 'torso', [-1, 21, 2])
        for side, sign in (('left', 1), ('right', -1)):
            for i in range(5):
                x = sign * (1.5 + i * 3)
                self.cube('glider_' + side, [x if sign > 0 else x - 3, 19.5 - i * .5, 2.4 + i * .5], [3.2, .55, 5 - i * .45], 'wing')
                self.cube('glider_' + side, [x if sign > 0 else x - 3, 20 - i * .5, 2.4 + i * .5], [3.2, .4, .65], 'accent')
        self.outfit()

    def pixel(self, x, y, color):
        assert 0 <= x < 128 and 0 <= y < 128
        self.pixels[(y * 128 + x) * 4:(y * 128 + x) * 4 + 4] = bytes((*color, 255))

    def rect(self, x, y, w, h, color):
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                self.pixel(xx, yy, color)

    def add(self, name, parent, pivot):
        bone = {'name': name, 'pivot': pivot}
        if parent:
            bone['parent'] = parent
        self.bones.append(bone)

    def cube(self, bone, origin, size, material, rotation=None):
        cube = {'origin': origin, 'size': size, 'uv': self.uvs[material].copy()}
        if rotation:
            cube['pivot'] = [origin[i] + size[i] / 2 for i in range(3)]
            cube['rotation'] = rotation
        target = next(b for b in self.bones if b['name'] == bone)
        target.setdefault('cubes', []).append(cube)

    def outfit(self):
        c = self.cube
        if self.name == 'aether':
            self.add('braid', 'hair', [.7, 27, 2.5])
            for i in range(7):
                c('braid', [.2 + (i % 2) * .35, 26 - i * 1.6, 2.4], [1.25, 1.8, 1.35], 'hair', [0, 0, (-1 if i % 2 else 1) * 12])
            c('braid', [.35, 14.5, 2.5], [1.25, .65, 1.4], 'accent')
            c('torso', [-3, 21.5, -1.9], [6, 1, 3.8], 'dark')
            c('cape', [-2.3, 16, 1.85], [3.8, 6, .55], 'dark')
            c('cape', [-2.3, 15.5, 1.9], [3.8, .75, .6], 'accent')
            c('hips', [-2.7, 8.5, -1.8], [2, 5.8, .55], 'cloth', [0, 0, -10])
            c('hips', [.7, 10, -1.8], [2, 4.3, .55], 'cloth', [0, 0, 8])
        elif self.name == 'amber':
            for sign in (-1, 1):
                c('accessory', [sign * 2 - .8, 29, -.7], [1.6, 5.2, .9], 'cloth', [0, 0, -sign * 20])
                c('accessory', [sign * 2 - .45, 29.8, -1.18], [.9, 3.4, .25], 'dark', [0, 0, -sign * 20])
                c('hair', [sign * 2.5 - .7, 18.5, 1.7], [1.4, 6.5, 1.5], 'hair', [0, 0, sign * 8])
                c('accessory', [sign * 1.5 - 1, 28, -2.9], [2, 1.5, .5], 'accent')
                c('accessory', [sign * 1.5 - .7, 28.25, -3.1], [1.4, 1, .25], 'metal')
            c('accessory', [-.6, 28.5, -2.95], [1.2, .4, .5], 'dark')
            c('torso', [-2.2, 15, -1.87], [4.4, 4.2, .3], 'white')
            c('cape', [-2, 17.5, 1.7], [4, 4.5, .65], 'cloth')
            c('torso', [1.3, 19, -2], [1.5, 1.5, .45], 'accent')
            c('hips', [-2.7, 10, -1.65], [5.4, 3, 3.3], 'dark')
        elif self.name == 'kaeya':
            c('head', [-2.9, 25.4, -2.7], [2.25, 1.8, .35], 'black')
            c('head', [-3.2, 26.8, -2.68], [6.4, .35, .2], 'black', [0, 0, 10])
            c('hair', [-3, 21, 1.9], [2.4, 6.5, 1.2], 'hair')
            c('torso', [-2.95, 15, -1.9], [2.3, 7, .5], 'accent')
            c('torso', [-.5, 15.5, -1.85], [2.9, 5.5, .3], 'dark')
            c('cape', [.2, 10.5, 1.8], [4.3, 11.5, .7], 'accent')
            c('cape', [.45, 10, 2], [3.8, .7, .6], 'white')
            for i in range(6):
                c('torso', [-3.2 + i * 1.1, 21.5 + (i % 2) * .25, 1.1], [1.3, 1.5, 1.6], 'white', [0, 0, (-1 if i % 2 else 1) * 12])
            for sign in (-1, 1):
                c('hips', [sign * 1.7 - 1.3, 7.5, 1.2], [2.6, 6, .6], 'cloth')
        else:
            c('accessory', [-6.8, 28.7, -5.7], [13.6, .65, 11.4], 'cloth')
            c('accessory', [-4.3, 29.2, -3.8], [8.6, .65, 7.6], 'accent')
            c('accessory', [-3.5, 29.7, -3], [7, 2.3, 6], 'cloth')
            c('accessory', [-2.4, 31.9, -2], [4.8, 1.9, 4], 'cloth')
            c('accessory', [-.9, 33.6, -1.1], [2.4, 1.8, 2.3], 'cloth', [0, 0, -22])
            for i in range(3):
                c('accessory', [3.2 + i * .45, 29.4 + i * .6, -3.2], [1.2, 1.2, 1], 'rose', [0, 0, i * 25])
            for sign in (-1, 1):
                c('hair', [sign * 2.7 - .9, 18.8, 1.4], [1.8, 6.5, 1.7], 'hair', [0, 0, sign * 10])
                c('hips', [sign * 1.7 - 1.65, 8.8, -1.8], [3.3, 5.2, 3.7], 'cloth', [0, 0, -sign * 7])
                c('hips', [sign * 1.7 - 1.7, 8.6, -1.9], [3.4, .7, 3.9], 'accent')
            c('torso', [-1.8, 20.2, -1.83], [3.6, 2.5, .4], 'skin')
            c('torso', [-.7, 19.4, -2], [1.4, 1.4, .4], 'rose', [0, 0, 45])
            c('cape', [-2.3, 18, 1.8], [4.6, 4, .6], 'cloth')
        if self.name in ('aether', 'kaeya'):
            c('weapon', [-4.15, 11, -.35], [.7, 3.6, .7], 'dark')
            c('weapon', [-6.1, 10.3, -.5], [4.6, .7, 1], 'accent')
            c('weapon', [-4.5, 1.8, -.3], [1.4, 8.5, .6], 'metal')
            c('weapon', [-4.15, .5, -.3], [.7, 1.5, .6], 'metal')
            c('weapon', [-4.15, 2.3, -.45], [.3, 7.5, .15], 'accent')
        elif self.name == 'amber':
            c('weapon', [3.5, 10.8, -.6], [.6, 3.5, .7], 'dark')
            for sign in (-1, 1):
                c('weapon', [3.35, 12.5 + sign * 3.9 - 1.8, -.2], [.8, 3.6, 1], 'accent', [sign * 25, 0, 0])
                c('weapon', [3.4, 12.5 + sign * 6 - .5, 1], [.7, 1.4, 1.3], 'cloth')
            self.add('bow_string', 'weapon', [3.8, 12.5, 2.2])
            c('bow_string', [3.68, 6.4, 2.2], [.18, 12.2, .18], 'white')
        else:
            c('weapon', [2.1, 11.6, -1.8], [3.4, .8, 4.2], 'dark')
            c('weapon', [2.25, 12.35, -1.65], [3.1, .6, 3.9], 'white')
            c('weapon', [2.1, 12.9, -1.8], [3.4, .3, 4.2], 'cloth')
            c('weapon', [3.25, 13.21, -.5], [1.1, .12, 1.6], 'accent')

    def geometry(self):
        names = {b['name'] for b in self.bones}
        assert len(names) == len(self.bones)
        visited = set()
        for b in self.bones:
            assert 'parent' not in b or b['parent'] in visited, 'Parent cycle/missing parent'
            visited.add(b['name'])
            for cube in b.get('cubes', []):
                assert all(math.isfinite(n) for n in cube['origin'] + cube['size'])
                assert all(n > 0 for n in cube['size'])
                for uv in cube['uv'].values():
                    assert all(0 <= uv['uv'][i] and uv['uv'][i] + uv['uv_size'][i] <= 128 for i in (0, 1))
        return {'format_version': '1.12.0', 'minecraft:geometry': [{'description': {
            'identifier': 'geometry.genshin.' + self.name, 'texture_width': 128, 'texture_height': 128,
            'visible_bounds_width': 4, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 1.2, 0]}, 'bones': self.bones}]}


def animations(model):
    result = {}
    for name, (length, loop) in PHASES.items():
        # Every clip resets the articulated skeleton; phase changes cannot inherit stale limb poses.
        tracks = {b['name']: {'rotation': [0, 0, 0]} for b in model.bones}
        def keys(bone, values, channel='rotation'):
            assert all(0 <= f <= length and all(math.isfinite(x) for x in v) for f, v in values)
            assert all(values[i][0] < values[i + 1][0] for i in range(len(values) - 1))
            tracks[bone][channel] = {str(f / 60): v for f, v in values}
        def pose(bone, value):
            tracks[bone]['rotation'] = value
        if name == 'idle':
            keys('torso', [(0, [0, 0, -1]), (60, [1.5, 0, 1]), (120, [0, 0, -1])])
            pose('right_arm', [0, 0, 6]); pose('left_arm', [0, 0, -6])
        elif name in ('walk', 'run'):
            amp = 26 if name == 'walk' else 48
            for side, sign in (('right', 1), ('left', -1)):
                keys(side + '_leg', [(0, [amp * sign, 0, 0]), (length / 2, [-amp * sign, 0, 0]), (length, [amp * sign, 0, 0])])
                keys(side + '_arm', [(0, [-amp * sign * .65, 0, sign * 6]), (length / 2, [amp * sign * .65, 0, sign * 6]), (length, [-amp * sign * .65, 0, sign * 6])])
                keys(side + '_shin', [(0, [8, 0, 0]), (length / 4, [28, 0, 0]), (length / 2, [8, 0, 0]), (length * .75, [2, 0, 0]), (length, [8, 0, 0])])
            keys('root', [(0, [0, 0, 0]), (length / 4, [0, .4, 0]), (length / 2, [0, 0, 0]), (length * .75, [0, .4, 0]), (length, [0, 0, 0])], 'position')
            pose('torso', [5 if name == 'walk' else 12, 0, 0])
        elif name == 'dash':
            keys('root', [(0, [0, 0, 0]), (3, [0, -1.8, 0]), (12, [0, -1.2, 0]), (18, [0, 0, 0])], 'position')
            keys('torso', [(0, [0, 0, 0]), (3, [38, 0, 0]), (12, [30, 0, 0]), (18, [8, 0, 0])])
            pose('right_arm', [50, 0, 12]); pose('left_arm', [40, 0, -15])
            pose('right_leg', [-40, 0, 0]); pose('left_leg', [45, 0, 0]); pose('left_shin', [55, 0, 0])
        elif name in ('jump', 'fall'):
            pose('right_arm', [-25, 0, 22]); pose('left_arm', [-25, 0, -22])
            pose('right_leg', [-18, 0, 0]); pose('left_leg', [12, 0, 0]); pose('right_shin', [28, 0, 0])
            keys('torso', [(0, [-8, 0, 0]), (length / 2, [3, 0, 0]), (length, [-8 if loop else 8, 0, 0])])
        elif name.startswith('land') or name == 'mantle':
            depth = 3 if name == 'land_soft' else 5
            keys('root', [(0, [0, -depth, 0]), (length / 3, [0, -depth, 0]), (length, [0, 0, 0])], 'position')
            for side in ('left', 'right'):
                keys(side + '_leg', [(0, [-25, 0, 0]), (length, [0, 0, 0])])
                keys(side + '_shin', [(0, [55, 0, 0]), (length, [0, 0, 0])])
                keys(side + '_arm', [(0, [-65 if name == 'mantle' else -15, 0, 0]), (length, [0, 0, 0])])
            keys('torso', [(0, [24, 0, 0]), (length, [0, 0, 0])])
        elif name.startswith('climb'):
            pose('torso', [-8, 0, 0]); pose('right_arm', [-140, 0, -10]); pose('left_arm', [-145, 0, 10])
            pose('right_forearm', [-25, 0, 0]); pose('left_forearm', [-30, 0, 0])
            pose('right_leg', [-35, 0, -12]); pose('left_leg', [-30, 0, 12])
            pose('right_shin', [70, 0, 0]); pose('left_shin', [65, 0, 0])
            if name != 'climb_idle':
                sign = -1 if name in ('climb_down', 'climb_right') else 1
                keys('right_arm', [(0, [-140, 0, -10]), (length / 2, [-100, 0, -10 - sign * 20]), (length, [-140, 0, -10])])
                keys('left_arm', [(0, [-100, 0, 10 + sign * 20]), (length / 2, [-145, 0, 10]), (length, [-100, 0, 10 + sign * 20])])
                keys('right_leg', [(0, [-50, 0, -12]), (length / 2, [-15, 0, -12]), (length, [-50, 0, -12])])
                keys('left_leg', [(0, [-15, 0, 12]), (length / 2, [-50, 0, 12]), (length, [-15, 0, 12])])
            if name == 'climb_jump':
                keys('torso', [(0, [15, 0, 0]), (6, [-20, 0, 0]), (18, [-8, 0, 0])])
        elif name.startswith('glide'):
            pose('torso', [12, 0, 0]); pose('right_arm', [-15, 0, 65]); pose('left_arm', [-15, 0, -65])
            pose('right_leg', [10, 0, 0]); pose('left_leg', [10, 0, 0]); pose('right_shin', [15, 0, 0]); pose('left_shin', [15, 0, 0])
            for side, sign in (('left', 1), ('right', -1)):
                if name == 'glide_start':
                    keys('glider_' + side, [(0, [0, sign * 82, sign * 40]), (9, [0, sign * 15, 0]), (18, [0, sign * 4, 0])])
                    keys('glider_' + side, [(0, [.05, .05, .05]), (6, [.5, .5, .5]), (18, [1, 1, 1])], 'scale')
                elif name == 'glide_stop':
                    keys('glider_' + side, [(0, [1, 1, 1]), (length, [.001, .001, .001])], 'scale')
                    keys(side + '_arm', [(0, [-15, 0, -sign * 65]), (length, [0, 0, -sign * 6])])
                else:
                    keys('glider_' + side, [(0, [0, sign * 4, sign * 2]), (45, [0, sign * 7, -sign * 2]), (90, [0, sign * 4, sign * 2])])
        elif name == 'swim':
            pose('root', [75, 0, 0])
            for side, sign in (('right', 1), ('left', -1)):
                keys(side + '_arm', [(0, [-80, 0, sign * 20]), (30, [40, 0, sign * 50]), (60, [-80, 0, sign * 20])])
                keys(side + '_leg', [(0, [sign * 20, 0, 0]), (30, [-sign * 20, 0, 0]), (60, [sign * 20, 0, 0])])
        if not name.startswith('glide'):
            for side in ('left', 'right'):
                tracks['glider_' + side]['scale'] = [.001, .001, .001]
        cape = 28 if name.startswith('glide') else 20 if name in ('dash', 'run') else 5
        keys('cape', [(0, [-cape, 0, -2]), (length / 2, [-cape - 6, 0, 2]), (length, [-cape, 0, -2])])
        if 'braid' in tracks:
            keys('braid', [(0, [-cape / 2, 0, 3]), (length / 2, [-cape / 2 - 4, 0, -3]), (length, [-cape / 2, 0, 3])])
        result['locomotion.' + name] = {'loop': loop, 'animation_length': length / 60, 'bones': tracks}
    result.update(combat_animations(model))
    return {'format_version': '1.8.0', 'animations': result, 'genshin_field_events': field_events(model.name)}


def field_events(name):
    # Independent impacts may occur AFTER cast recovery; do not lengthen the player's motion.
    anchors = {'amber': ('AmberKit', ('ADAPTED_PUPPET_LANDING_FRAME',)),
               'lisa': ('LisaKit', ('TAP_FIRST_IMPACT_FRAME', 'ADAPTED_ROSE_FORMATION_FRAME', 'ROSE_FIRST_DISCHARGE_FRAME'))}
    if name not in anchors:
        return {}
    cls, constants = anchors[name]
    source = (ROOT / f'common/src/main/java/io/github/brainage04/genshininminecraft/rules/kit/{cls}.java').read_text()
    result = {}
    for constant in constants:
        match = re.search(r'\b' + constant + r'\s*=\s*(\d+)\s*;', source)
        assert match, f'Missing independent field anchor {constant}'
        result[constant] = int(match[1]) / 60
    return result


def combat_timings(name):
    """Read the small Java descriptors, not a second copy of the kits' frame tables."""
    java = ROOT / 'common/src/main/java/io/github/brainage04/genshininminecraft/rules'
    classes = ('TravelerAnemoKit', 'AmberKit', 'KaeyaKit', 'LisaKit')
    slot = NAMES.index(name)
    sources = {cls: (java / f'kit/{cls}.java').read_text() for cls in classes}
    def scalar(value):
        if re.fullmatch(r'-?\d+', value):
            return int(value)
        cls, constant = value.split('.')
        match = re.search(r'\b' + constant + r'\s*=\s*(\d+)\s*;', sources[cls])
        assert match, f'Expected numeric kit anchor {value}'
        return int(match[1])
    def array(field):
        match = re.search(r'\b' + field + r'\s*=\s*\{([\d, ]+)\}', sources[classes[slot]])
        assert match, f'Missing normal table {field}'
        return [int(value) for value in match[1].split(',')]
    hits = array(('NORMAL_HIT_FRAMES', 'RELEASE_FRAMES', 'HIT_FRAMES', 'HIT_FRAMES')[slot])
    recoveries = array('NORMAL_RECOVERY_FRAMES' if slot == 0 else 'RECOVERY_FRAMES')
    result = {f'n{i + 1}': (hit, -1, recovery, False) for i, (hit, recovery) in enumerate(zip(hits, recoveries))}
    descriptor = (java / 'CombatAnimations.java').read_text()
    for match in re.finditer(r'put\((\d), Action\.(\w+), ([\w.-]+), ([\w.-]+), ([\w.-]+), (true|false)\);', descriptor):
        if int(match[1]) == slot:
            result[match[2].lower()] = (scalar(match[3]), scalar(match[4]), scalar(match[5]), match[6] == 'true')
    # Authored, not researched, universal additive recoil and final fallen pose.
    result['hurt'] = (3, -1, 18, False)
    result['fallen'] = (30, -1, 60, False)
    return result


def combat_animations(model):
    result = {}
    upper = ('torso', 'head', 'hair', 'accessory', 'right_arm', 'right_forearm', 'right_hand',
             'left_arm', 'left_forearm', 'left_hand', 'weapon', 'cape')
    upper += tuple(b['name'] for b in model.bones if b['name'] in ('braid', 'bow_string'))
    for action, (strike, second, length, loop) in combat_timings(model.name).items():
        tracks = {bone: {'rotation': [0, 0, 0], 'position': [0, 0, 0], 'scale': [1, 1, 1]} for bone in upper}
        def keys(bone, values, channel='rotation'):
            assert all(0 <= frame <= length and all(math.isfinite(x) for x in value) for frame, value in values)
            assert all(values[i][0] < values[i + 1][0] for i in range(len(values) - 1)), (action, bone, values)
            tracks.setdefault(bone, {})[channel] = {str(frame / 60): value for frame, value in values}
        def gesture(bone, wind, hit, rest=(0, 0, 0)):
            if strike == 0:
                values = [(0, hit), (length, list(rest))]
            else:
                values = [(0, list(rest)), (max(1, strike * .65), wind), (strike, hit)]
                if strike < length:
                    values.append((length, list(rest)))
            keys(bone, values)
        sword = model.name in ('aether', 'kaeya')
        if action.startswith('n'):
            n = int(action[1:]) - 1
            if sword:
                # Cross cut, reverse cut, rising cut, overhead chop, forward thrust: not one reused swing.
                winds = [(-45, -55, -65), (-55, 65, 60), (25, -25, -30), (-165, 0, 18), (-50, -20, 30)]
                hits = [(-70, 55, 55), (-60, -55, -50), (-135, 15, 30), (-65, 0, -18), (-90, 0, -5)]
                offset = 10 if model.name == 'kaeya' else 0
                gesture('right_arm', list(winds[n]), [hits[n][0] - offset, hits[n][1], hits[n][2]])
                gesture('right_forearm', [-55, 0, 0], [-12 if n != 4 else -5, 0, 0])
                gesture('torso', [-5, (-1 if n % 2 else 1) * -35, 8], [10, (-1 if n % 2 else 1) * 30, -8])
                gesture('left_arm', [-25, 0, -35], [15, 0, -25])
                gesture('weapon', [0, 0, -15], [0, 0, 20 if n < 4 else 0])
            elif model.name == 'amber':
                gesture('torso', [0, -35 + n * 4, 0], [4, -30 + n * 4, -3])
                gesture('left_arm', [-85, 0, -15 - n * 2], [-85, 0, -15 - n * 2])
                gesture('right_arm', [-80, -25, 35], [-65 + n * 3, -35, 55])
                gesture('right_forearm', [-95, 0, 0], [-45, 0, 0])
                gesture('weapon', [85, 0, 0], [85, 0, 0])
                keys('bow_string', [(0, [0, 0, 0]), (strike * .65, [0, 0, 2.2]), (strike, [0, 0, 0]), (length, [0, 0, 0])], 'position')
            else:
                gesture('right_arm', [-35 - n * 22, -25 + n * 15, 30], [-85 - n * 15, 25 - n * 15, 15 + n * 12])
                gesture('right_forearm', [-65, 0, 0], [-8, 0, 0])
                gesture('left_arm', [-45, 0, -20], [-50, 0, -25])
                gesture('weapon', [-25, 0, -15], [-35, 0, -20])
                gesture('torso', [-4, -20 + n * 12, 5], [4, 15 - n * 10, -5])
        elif action == 'charged':
            if sword:
                keys('torso', [(0, [0, -60, 0]), (strike, [10, 110, -8]),
                               (second if second > strike else strike + 6, [5, 300, 8]), (length, [0, 360, 0])])
                gesture('right_arm', [-50, -45, -70], [-85, 30, 80])
                gesture('right_forearm', [-40, 0, 0], [-10, 0, 0])
                gesture('left_arm', [-30, 0, -40], [20, 0, -60])
                if second > strike:
                    keys('right_arm', [(0, [-40, -25, -45]), (strike * .65, [-50, -45, -70]),
                                       (strike, [-85, 30, 80]), (second, [-95, -40, -70]), (length, [0, 0, 6])])
            else:
                gesture('right_arm', [-165, 0, 35], [-90, 0, 15])
                gesture('left_arm', [-75, 0, -35], [-70, 0, -55])
                gesture('weapon', [-45, 0, -15], [-30, 0, -20])
                gesture('torso', [-12, -15, 0], [15, 20, 0])
        elif action in ('aim_hold', 'aim_release'):
            draw = [(0, 0), (9, .5), (15, .85), (length, 1)] if action == 'aim_hold' else [(0, 1), (length, 0)]
            keys('left_arm', [(frame, [-85 * amount, 0, -18 * amount]) for frame, amount in draw])
            keys('right_arm', [(frame, [-80 * amount, -30 * amount, 48 * amount]) for frame, amount in draw])
            keys('right_forearm', [(frame, [-100 * amount, 0, 0]) for frame, amount in draw])
            keys('torso', [(frame, [0, -32 * amount, 0]) for frame, amount in draw])
            keys('weapon', [(frame, [85 * amount, 0, 0]) for frame, amount in draw])
            keys('bow_string', [(frame, [0, 0, 2.8 * amount]) for frame, amount in draw], 'position')
            if action == 'aim_release':
                keys('bow_string', [(0, [0, 0, 0]), (2, [0, 0, -.5]), (length, [0, 0, 0])], 'position')
        elif action in ('skill_start', 'skill_hold'):
            target = {'left_arm': [-95, 0, -25], 'right_arm': [-75, 0, 25],
                      'left_forearm': [-20, 0, 0], 'right_forearm': [-30, 0, 0], 'torso': [8, 0, 0]}
            if model.name == 'lisa':
                target |= {'left_arm': [-55, 0, -25], 'right_arm': [-140, 0, 35],
                           'weapon': [-35, 0, -20], 'torso': [-8, 0, 0]}
            for bone, pose in target.items():
                keys(bone, [(0, pose if loop else [0, 0, 0]), (length / 2, [pose[0] - 5, pose[1], pose[2]]), (length, pose)])
        elif action in ('skill_tap', 'skill_release'):
            if model.name == 'amber':
                gesture('right_arm', [-145, -20, 20], [-90, 0, 10])
                gesture('right_forearm', [-65, 0, 0], [-5, 0, 0])
                gesture('left_arm', [15, 0, -25], [20, 0, -30])
                gesture('torso', [-12, -30, 0], [16, 20, 0])
            elif model.name == 'kaeya':
                gesture('left_arm', [-45, -25, -30], [-90, 0, -12])
                gesture('left_forearm', [-65, 0, 0], [-5, 0, 0])
                gesture('right_arm', [-60, -20, 35], [-95, 0, 10])
                gesture('torso', [-8, -25, 0], [12, 20, 0])
            else:
                gesture('right_arm', [-130, 0, 30], [-95, 0, 12])
                gesture('right_forearm', [-45, 0, 0], [-5, 0, 0])
                gesture('left_arm', [-55, 0, -25], [-85 if model.name == 'aether' else -55, 0, -20])
                gesture('torso', [-8, -15, 0], [14, 15, 0])
                if model.name == 'lisa':
                    gesture('weapon', [-35, 0, -20], [-25, 0, -20])
        elif action == 'burst':
            if model.name == 'amber':
                gesture('left_arm', [-145, 0, -20], [-160, 0, -15])
                gesture('right_arm', [-130, -30, 40], [-140, -25, 55])
                gesture('right_forearm', [-100, 0, 0], [-25, 0, 0])
                gesture('weapon', [85, 0, 0], [85, 0, 0])
                gesture('head', [-25, 0, 0], [-35, 0, 0])
                keys('bow_string', [(0, [0, 0, 0]), (strike * .65, [0, 0, 2.8]), (strike, [0, 0, 0]), (length, [0, 0, 0])], 'position')
            elif model.name == 'kaeya':
                gesture('right_arm', [-65, -30, 20], [-165, 0, 15])
                gesture('left_arm', [-35, 0, -20], [-85, 0, -55])
                gesture('weapon', [0, 0, -30], [0, 0, 0])
                gesture('torso', [-8, -25, 0], [-5, 20, 0])
            elif model.name == 'lisa':
                gesture('left_arm', [-60, 0, -25], [-95, 0, -50])
                gesture('right_arm', [-150, -20, 25], [-165, 15, 35])
                gesture('weapon', [-35, 0, -20], [-60, 0, -25])
                gesture('torso', [-12, -25, 0], [-5, 20, 0])
            else:
                gesture('left_arm', [-145, -20, -35], [-95, 0, -15])
                gesture('right_arm', [-130, 20, 35], [-100, 0, 20])
                gesture('torso', [-15, -35, 0], [15, 35, 0])
        elif action == 'hurt':
            # Additive controller: zero is a neutral delta, not a competing full-body pose.
            tracks = {}
            keys('torso', [(0, [0, 0, 0]), (strike, [-12, 0, 8]), (length, [0, 0, 0])])
            keys('head', [(0, [0, 0, 0]), (strike, [-8, 0, -6]), (length, [0, 0, 0])])
        elif action == 'fallen':
            tracks = {bone['name']: {'rotation': [0, 0, 0], 'position': [0, 0, 0], 'scale': [1, 1, 1]} for bone in model.bones}
            keys('root', [(0, [0, 0, 0]), (strike, [0, 0, 80]), (length, [0, 0, 90])])
            keys('root', [(0, [0, 0, 0]), (strike, [0, 1, 0]), (length, [0, 1, 0])], 'position')
            keys('right_arm', [(0, [0, 0, 6]), (strike, [-30, 0, 25]), (length, [-30, 0, 25])])
            keys('left_arm', [(0, [0, 0, -6]), (strike, [-20, 0, -20]), (length, [-20, 0, -20])])
            for side in ('left', 'right'):
                tracks['glider_' + side]['scale'] = [.001, .001, .001]
        # Strike/release anchors are real bone keys and inert audit labels, never sound/gameplay callbacks.
        markers = {}
        if strike >= 0:
            markers[str(strike / 60)] = ['strike']
        if second >= 0:
            markers.setdefault(str(second / 60), []).append('strike.second')
        result['combat.' + action] = {'loop': loop, 'animation_length': length / 60, 'bones': tracks,
                                     'genshin_markers': {time: ' '.join(labels) for time, labels in markers.items()}}
    return result


def json_bytes(data):
    return (json.dumps(data, indent=2, ensure_ascii=False, allow_nan=False) + '\n').encode()


def outputs():
    files = {}
    for name in NAMES:
        model = Model(name)
        files[ASSETS / f'geckolib/models/character/{name}.geo.json'] = json_bytes(model.geometry())
        files[ASSETS / f'geckolib/animations/character/{name}.animation.json'] = json_bytes(animations(model))
        files[ASSETS / f'textures/entity/character/{name}.png'] = png(model.pixels)
    # This block is generated, so provenance is checked alongside the actual art.
    credits = ROOT / 'CREDITS.md'
    start, end = '<!-- character-assets:start -->', '<!-- character-assets:end -->'
    text = credits.read_text()
    block = '\n'.join([start, '', '## Original generated character assets (20a/20b)', '',
        '`tools/models/generate.py` authors the Aether, Amber, Kaeya and Lisa cuboid geometry,',
        '128×128 per-face shaded/pixel-painted textures, articulated weapons and original wind gliders,',
        'and all locomotion/combat key poses. Authored for this repository, 2026-10-08; no extracted game assets,',
        'downloaded fan meshes, traced textures, YiFang content or copied sound files. Outputs under',
        '`assets/genshininminecraft/geckolib/{models,animations}/character` and',
        '`textures/entity/character` are reproducible with Python 3 (stdlib only); `--check` compares bytes.',
        '', 'Public visual references (silhouette/colour/signature features only):',
        '- [Traveler](https://genshin-impact.fandom.com/wiki/Traveler)',
        '- [Amber](https://genshin-impact.fandom.com/wiki/Amber)',
        '- [Kaeya](https://genshin-impact.fandom.com/wiki/Kaeya)',
        '- [Lisa](https://genshin-impact.fandom.com/wiki/Lisa)',
        '', 'These are placeholder-quality original adaptations, not faithful source-game meshes/poses.',
        'All locomotion/combat sounds refer to built-in Minecraft events; no new audio assets.', '', end])
    if start in text:
        before, remaining = text.split(start, 1)
        _, after = remaining.split(end, 1)
        text = before + block + after
    else:
        text = text.rstrip() + '\n\n' + block + '\n'
    files[credits] = text.encode()
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true', help='compare generated assets/provenance without changing them')
    args = parser.parse_args()
    mismatches = []
    for path, data in outputs().items():
        if args.check:
            if not path.exists() or path.read_bytes() != data:
                mismatches.append(str(path.relative_to(ROOT)))
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(data)
    if mismatches:
        parser.exit(1, 'Generated files differ:\n' + '\n'.join(mismatches) + '\n')
    print('Character assets ' + ('match byte-for-byte' if args.check else 'generated') + ' (4 models, 4 animation sets, 4 textures, provenance).')


if __name__ == '__main__':
    main()
