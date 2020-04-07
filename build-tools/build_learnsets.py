# -*- coding: utf-8 -*-

from pyjsparser import parse
from common import *
from json import dumps

app_data_dir = "../app/src/main/res/raw"
target_file_name = "learnsets.json"
url_js_file = "http://play.pokemonshowdown.com/data/learnsets.js"
url_js_file2 = "http://play.pokemonshowdown.com/data/pokedex.js"


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

data = get_remote_data(url_js_file2)

log("Decoding JavaScript data...")
obj = parse(data)
properties = obj['body'][0]['expression']['right']['properties']
prevos = dict()
set_log_p()
for poke_entry in properties:
   species = poke_entry['key']['name']
   details_entries = poke_entry['value']['properties']
   log_p()
   for details_entry in details_entries:
       key_entry = details_entry['key']
       if 'prevo' == key_entry['name']:
           prevos[species] = details_entry['value']['value']  
           break
log("\nDone")


log("Computing learnsets...")

py_obj = dict()

def get_evolutions_rec(family):
    l = []
    for dexspecies in prevos:
        if prevos[dexspecies] in family[-1]:
            l.append(dexspecies)
    if len(l) > 0:
        family.append(l)
        return get_evolutions_rec(family)
    return family

set_log_p()
for species in learnsets:
    log_p()
    moves = []
    for move in learnsets[species]:
        moves.append(move)
        
    family = get_evolutions_rec([[species]])
    if len(family) > 1:
        py_obj[species] = {'moves':moves, 'also':[e for l in family[1:] for e in l]}
    else:
        py_obj[species] = {'moves':moves}
log("\nDone")


json_content = dumps(py_obj, sort_keys=True)
write_into_file(app_data_dir + "/" + target_file_name, json_content)

finish()