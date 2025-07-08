"""Command line interface for Surpass Assist."""

import argparse
from pathlib import Path

import supervision as sv

from .video_processing import process_video


DEFAULT_CLASSES = {
    1: "bicycle",
    2: "car",
    3: "motorcycle",
    4: "airplane",
    5: "bus",
    6: "train",
    7: "truck",
    15: "cat",
    16: "dog",
    17: "horse",
    18: "sheep",
    19: "cow",
}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Process a video using Surpass Assist")
    parser.add_argument("video", help="Path to input video")
    parser.add_argument("model", help="Path to YOLO model")
    parser.add_argument("lane_model", help="Path to lane detection weights")
    parser.add_argument("output", help="Path to save annotated video")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    classes = DEFAULT_CLASSES
    class_indexes = list(classes.keys())
    box_annotator = sv.BoxAnnotator(thickness=2, text_thickness=2, text_scale=1)

    process_video(
        Path(args.video),
        Path(args.model),
        Path(args.lane_model),
        Path(args.output),
        class_indexes,
        classes,
        box_annotator,
    )


if __name__ == "__main__":
    main()
