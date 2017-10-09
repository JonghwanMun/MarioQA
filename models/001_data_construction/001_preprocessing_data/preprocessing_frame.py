import os
import io
import base64
import random
import string
import argparse

import json
from tqdm import tqdm
import numpy as np
from PIL import Image

def load_json(file_path):
    with open(file_path, 'r') as f:
        json_file = json.load(f)
    return json_file

def load_image_from_base64String(img_path):
    img = base64.b64decode(open(img_path, "rb").read())
    buf = io.BytesIO(img)
    return Image.open(buf)

def prepro(params):
    """ preprocessing the frames
    """

    if not os.path.exists(params['out_dir']):
        os.mkdir(params['out_dir'])

    print("===> Loading annotation file from: " + params['ann_path'])
    anns = load_json(params['ann_path'])

    print("===> Resizing images")
    for ia, ann in enumerate(tqdm(anns)):

        bf = ann['begin_frame']
        ef = ann['end_frame']
        frame_path = os.path.join(params['data_dir'], ann['video_path'])
        save_to = os.path.join(params['out_dir'], ann['video_path']).replace(".dat", ".png")
        resized_dir = os.path.join(params['out_dir'], ann['video_path'].split('/')[0])

        if not os.path.exists(resized_dir):
            os.mkdir(resized_dir)
            print("Create folder: " + resized_dir)

        for fr in range(bf, ef+1):

            if not os.path.exists(save_to % fr):
                img = load_image_from_base64String(frame_path % fr)
                img = img.resize((params['width'], params['height']), Image.BICUBIC)
                img.save(save_to % fr)


if __name__ == "__main__":
    parser = argparse.ArgumentParser()

    # input json
    parser.add_argument('--data_dir', default='data/gameplay', 
            help='path to root directory of gameplay clips')
    parser.add_argument('--ann_path',
                        default='data/generated_annotation/filtered_annotations_ALL.json', 
            help='path to filtered annotations (All)')
    parser.add_argument('--out_dir', default='data/mario_resized_frames', 
            help='path to the directory for preprocessed clips')
    parser.add_argument('--width', default=160, type=int, help='width of resized image')
    parser.add_argument('--height', default=120, type=int, help='height of resized image')

    args = parser.parse_args()
    params = vars(args) # convert to ordinary dict
    print 'parsed input parameters:'
    print json.dumps(params, indent = 2)

    # run preprocessing
    prepro(params)
