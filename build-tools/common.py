# -*- coding: utf-8 -*-

import requests
import time

def int_val(v):
    if type(v) is bool:
        return v
    try:
        if int(v) == v:
            return int(v)
        else:
            return v
    except ValueError:
        return v
    
def log(text):
    print(text)
    
i = 0

def set_log_p():
    global i
    i = 0
    
def log_p():
    global i
    i += 1
    print(i, end='\r')
    
def get_remote_data(url, utf8=True):
    print("Fetching {} content...".format(url))
    time1 = time.time()
    data = requests.get(url)
    time2 = time.time()
    print("Fetched {} bytes in {:.3f} ms".format(len(data.content), (time2-time1)*1000.0))
    print("Done") 
    if utf8 == True:
        return data.content.decode("utf-8")
    else:
        return data.content
	
def write_into_file(path, data, binary=False):
    if (input("Override {} ? [y/n]".format(path)).strip() != "y"):
        return False
    print("Writing data into " + path + "...")
    time1 = time.time()
    if binary == True:
        mode = "wb"
    else:
        mode = "w"
    file = open(path, mode) 
    file.write(data)
    file.close() 
    time2 = time.time()
    print("Wrote {} bytes in {:.3f} ms".format(len(data), (time2-time1)*1000.0))
    return True

def safe_key_name(key_element):
    if 'name' in key_element.keys():
        return key_element['name']
    else:
        return key_element['value']
	
def finish():
	print("")
	print("Finished")
	input("Press enter to close this window")