# -*- coding: utf-8 -*-

from common import *

app_data_dir = "../psclient/src/main/res/raw"
target_file_name = "dex_icons_sheet.png"
url_png_file = "http://play.pokemonshowdown.com/sprites/pokemonicons-sheet.png"

data = get_remote_data(url_png_file, utf8=False)

write_into_file(app_data_dir + "/" + target_file_name, data, binary=True)

finish()