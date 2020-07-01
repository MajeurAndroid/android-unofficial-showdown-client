# -*- coding: utf-8 -*-

from pyjsparser import parse
from common import *
from json import dumps

app_data_dir = "../psclient/src/main/res/raw"
target_file_name = "moves.json"
url_js_file = "http://play.pokemonshowdown.com/data/moves.js"

data = get_remote_data(url_js_file)

log("Decoding JavaScript data...")
obj = parse(data)
keys = ["accuracy","basePower","category","desc","shortDesc","type","priority","name","pp","zMovePower","target","zMoveEffect","gmaxPower"]
properties = obj['body'][0]['expression']['right']['properties']
moves = dict()   
set_log_p() 
i=0
for move_entry in properties:
    log_p()
    move_name = safe_key_name(move_entry['key'])
    moves[move_name] = dict()
    details_entries = move_entry['value']['properties']
    for details_entry in details_entries:
        key_entry = details_entry['key']
        if key_entry['name'] not in keys:
            continue 
        value_entry = details_entry['value']
        if value_entry['type'] == 'ArrayExpression':
            array = []
            for element in value_entry['elements']:
                array.append(element['value'])
            moves[move_name][key_entry['name']] = array
        elif value_entry['type'] == 'ObjectExpression':
            jsobj = dict()
            for prop in value_entry['properties']:
                jsobj[safe_key_name(prop['key'])] = int_val(prop['value']['value'])               
            moves[move_name][key_entry['name']] = jsobj
        elif value_entry['type'] == 'UnaryExpression':
            continue # Negative dex number -> pass
        else:
            moves[move_name][key_entry['name']] = int_val(details_entry['value']['value']) 
log("\nDone")

json_content = dumps(moves)
write_into_file(app_data_dir + "/" + target_file_name, json_content)

finish()