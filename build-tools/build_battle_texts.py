# -*- coding: utf-8 -*-

from pyjsparser import parse
from common import *
from json import dumps

app_data_dir = "../psclient/src/main/res/raw"
target_file_name = "battle_texts.json"
url_js_file = "http://play.pokemonshowdown.com/data/text.js"

data = get_remote_data(url_js_file)

log("Decoding JavaScript data...")
obj = parse(data)

properties = obj['body'][0]['expression']['right']['properties']
texts = dict()   
set_log_p() 
for text_entry in properties:
    log_p()
    name = text_entry['key']['name']
    
    vtype = text_entry['value']['type']
    if vtype == 'Literal':
        text[name] = text_entry['value']['value']
    elif vtype == 'ObjectExpression':
        texts[name] = dict()
        for sub_entry in text_entry['value']['properties']:
            texts[name][sub_entry['key']['name']] = sub_entry['value']['value']
log("\nDone")

json_content = dumps(texts)
write_into_file(app_data_dir + "/" + target_file_name, json_content)

finish()