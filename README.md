# Surpass Assist: Transforming Road Safety

## Introduction

Surpass Assist is a computer vision-based solution designed to significantly enhance road safety. It uses advanced machine learning algorithms to analyze road conditions, detect lanes, track vehicles, and gather critical data to ascertain the best and safest opportunities for vehicles to overtake large trucks. The overarching goal is to minimize the occurrence of accidents and alleviate traffic congestion caused by unsafe overtaking maneuvers.

## Features

- **Vehicle and Lane Detection:** Surpass Assist identifies vehicles and lanes on the road accurately, even under varying weather and lighting conditions.
- **Distance Tracking:** The system tracks the distance between vehicles to ensure safe overtaking.
- **Real-Time Data Analysis:** Surpass Assist collects and processes data in real-time for immediate response and decision-making.
- **Safety Alert System:** Alerts are relayed to drivers on when it's safe to overtake the large trucks.

## Lane Detection Model

The lane detection component relies on the open-source
[Ultra-Fast-Lane-Detection](https://github.com/cfzd/Ultra-Fast-Lane-Detection)
(UFLD) network. Download the pretrained weights (for example
`culane_18.pth`) from the project's releases:

```bash
wget https://github.com/cfzd/Ultra-Fast-Lane-Detection/releases/download/v1.0.0/culane_18.pth
```

Provide the path to the downloaded file when running the command line tool.

## Command Line Usage

After installing the dependencies, you can process a video directly from the
terminal:

```bash
python -m surpassassist <input_video> <model_path> <lane_model> <output_video>
```

The command loads the YOLO model, processes the video, and saves the annotated
output to the location you specify. The lane model path should point to the
pretrained UFLD weights you downloaded earlier.

## Try The Alpha Version

Sicne the project is in it's infency we only a small demo available for the public: [![Open In Colab](https://colab.research.google.com/assets/colab-badge.svg)](https://colab.research.google.com/drive/1r0lJmQfBUE35CoIMFeC3QKnnIfxU29P9?usp=sharing)

## Demo Video
[![Surpass Assist Demo Video](http://img.youtube.com/vi/C8QxjVD6yD8/0.jpg)](http://www.youtube.com/watch?v=C8QxjVD6yD8 "Surpass Assist Demo Video")

![Team OptiVision Banner](https://cdn.discordapp.com/attachments/1126133480204554292/1127247949433409536/didhx635-removebg-preview.png)
### Team Members

- [Tarek Zain](https://github.com/zain-codes)
- [Maan Sulaimani](https://github.com/maans2001)
- [RassanMa](https://github.com/RassanMa)
- [Suad Abu Shhadeh](https://www.linkedin.com/in/suad-abu-shhadeh-961600198)
- [RayanBeshawri](https://www.linkedin.com/in/rayan-beshawri)
- [Joory Abdulfattah](https://www.linkedin.com/in/joory-abdulfattah-477a92252)
