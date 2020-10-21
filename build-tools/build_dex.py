# -*- coding: utf-8 -*-

from pyjsparser import parse
from common import *
from json import dumps

app_data_dir = "../psclient/src/main/res/raw"
target_file_name = "dex.json"
url_js_file = "http://play.pokemonshowdown.com/data/pokedex.js"

data = get_remote_data(url_js_file)

log("Decoding JavaScript data...")
obj = parse(data)
keys = ["num", "name", "types", "baseStats", "abilities", "heightm", "weightkg", "color", "gender", "LC", "evos", "requiredItem"]
properties = obj['body'][0]['expression']['right']['properties']
dex = dict()   
set_log_p() 
for poke_entry in properties:
    log_p()
    species = poke_entry['key']['name']
    dex[species] = dict()
    details_entries = poke_entry['value']['properties']
    for details_entry in details_entries:
        key_entry = details_entry['key']
        if key_entry['name'] not in keys:
            continue 
        value_entry = details_entry['value']
        if value_entry['type'] == 'ArrayExpression':
            array = []
            for element in value_entry['elements']:
                array.append(element['value'])
            dex[species][key_entry['name']] = array
        elif value_entry['type'] == 'ObjectExpression':
            jsobj = dict()
            for prop in value_entry['properties']:
                jsobj[safe_key_name(prop['key'])] = int_val(prop['value']['value'])                  
            dex[species][key_entry['name']] = jsobj
        elif value_entry['type'] == 'UnaryExpression':
            continue # Negative dex number -> pass
        else:
            dex[species][key_entry['name']] = int_val(details_entry['value']['value']) 
log("\nDone")

json_content = dumps(dex)
write_into_file(app_data_dir + "/" + target_file_name, json_content)

finish()