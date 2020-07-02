# -*- coding: utf-8 -*-

from pyjsparser import parse
from common import *
from json import dumps

app_data_dir = "../psclient/src/main/res/raw"
target_file_name = "learnsets.json"
url_js_file = "http://play.pokemonshowdown.com/data/learnsets.js"

data = get_remote_data(url_js_file)

log("Decoding JavaScript data...")
obj = parse(data)
properties = obj['body'][0]['expression']['right']['properties']
learnsets = dict()
set_log_p()
for poke_entry in properties:
   species = poke_entry['key']['name']
   learnset_entry = poke_entry['value']['properties'][0]
   has_learnset = learnset_entry['value']['type'] == 'ObjectExpression'
   if has_learnset:
       move_entries = learnset_entry['value']['properties']
       moves = []
       for move_entry in move_entries:    
           log_p()
           key_entry = move_entry['key']
           if 'raw' in key_entry.keys():
               moves.append(move_entry['key']['raw']) # 'return' move case
           else:
               moves.append(move_entry['key']['name'])           
       learnsets[species] = moves
   else:
       base_species = ''
       for s in learnsets.keys():
           if species.startswith(s) and species != s:
               base_species = s
       if len(base_species) > 0:
           learnsets[species] = learnsets[base_species]
           log("No learnset for {}, using {}'s learnset.".format(species, base_species))
       else:
           log("No learnset for {}. No other related species found. Passing.".format(species))
log("\nDone")


json_content = dumps(learnsets)
write_into_file(app_data_dir + "/" + target_file_name, json_content)

finish()