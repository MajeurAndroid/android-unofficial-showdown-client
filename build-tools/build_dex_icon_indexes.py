# -*- coding: utf-8 -*-

from pyjsparser import parse
from common import *
from json import dumps

app_data_dir = "../app/src/main/res/raw"
target_file_name = "dex_icon_indexes.json"
url_js_file = "http://play.pokemonshowdown.com/data/pokedex-mini.js"
url_js_file2 = "http://raw.githubusercontent.com/smogon/pokemon-showdown-client/master/src/battle-dex-data.ts"

data = get_remote_data(url_js_file)

log("Decoding JavaScript data...")
obj = parse(data)

properties = obj['body'][0]['expression']['right']['properties']
indexes = dict()  
set_log_p() 
for dex_entry in properties:
    log_p()
    name = dex_entry['key']['name']    
    vtype = dex_entry['value']['type']
    if vtype == 'ObjectExpression':
        for sub_entry in dex_entry['value']['properties']:
            if sub_entry['key']['name'] == "num" and sub_entry['value']['type'] == 'Literal':    
                indexes[name] = int_val(sub_entry['value']['value'])
log("\nDone")

data = get_remote_data(url_js_file2)

log("Decoding JavaScript data...")
start_index = data.index('BattlePokemonIconIndexes:') + len("BattlePokemonIconIndexes:")
content = data[start_index:]
content = content[content.index("=")+1:]
content = content[:content.index('}')+1]
# Here we cannot use a js parser because of int operations and file written in ts
set_log_p()
for line in content.split("\n"):
    log_p()
    line = line.strip()
    if line[:2] == "//" or ':' not in line: 
        continue
    sep = line.index(':')
    key = line[:sep]
    ope = line[sep+1:-1].strip()
    nums = ope.split('+')
    index = 0
    for num in nums:
        index += int(num)
    indexes[key] = index


json_content = dumps(indexes)
write_into_file(app_data_dir + "/" + target_file_name, json_content)

finish()