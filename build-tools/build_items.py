# -*- coding: utf-8 -*-

from pyjsparser import parse
from common import *
from json import dumps

app_data_dir = "../app/src/main/res/raw"
target_file_name = "items.json"
url_js_file = "http://play.pokemonshowdown.com/data/items.js"

data = get_remote_data(url_js_file)

log("Decoding JavaScript data...")
obj = parse(data)

properties = obj['body'][0]['expression']['right']['properties']
items = dict()   
keys = ["name", "id", "desc", "spritenum"]
set_log_p() 
for item_entry in properties:
    log_p()
    name = item_entry['key']['name']    
    vtype = item_entry['value']['type']
    if vtype == 'ObjectExpression':
        items[name] = dict()
        for sub_entry in item_entry['value']['properties']:
            pname = sub_entry['key']['name']
            if pname not in keys:
                continue
            items[name][pname] = int_val(sub_entry['value']['value'])
log("\nDone")

json_content = dumps(items)
write_into_file(app_data_dir + "/" + target_file_name, json_content)

finish()