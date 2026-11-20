#!/usr/bin/env python3
"""Comprueba límites/formato; no sustituye la compilación ni pruebas del mod."""
import json, re, sys
from pathlib import Path

def validate(path):
    data = json.loads(Path(path).read_text(encoding='utf-8'))
    assert re.fullmatch(r'[a-z0-9_-]{1,48}', data['id']), 'ID inválido'
    assert 40 <= data['duration'] <= 72000
    assert len(data.get('actors',[])) <= 20
    assert len(data.get('events',[])) <= 256
    for actor in data.get('actors',[]):
        assert actor['entity'] == 'viewer' or ':' in actor['entity']
        assert 0 <= actor['x'] <= 1 and 0 <= actor['y'] <= 1
    for event in data.get('events',[]):
        assert event['kind'] in ('RHYTHM','QTE','CHOICE')
        assert 0 <= event['tick'] < data['duration']
        assert 0 <= event['x'] <= 1 and 0 <= event['y'] <= 1
        assert 1 <= event['window'] <= 40
    print(f"OK: {path} ({len(data.get('actors',[]))} actores, {len(data.get('events',[]))} eventos)")

if __name__ == '__main__':
    for path in sys.argv[1:] or ['examples/templo_escape.json']:
        validate(path)
