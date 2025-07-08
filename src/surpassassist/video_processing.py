"""Video processing utilities."""

from pathlib import Path

import cv2
import numpy as np
import ultralytics
import supervision as sv
from tqdm import tqdm

from .distance import calculate_distance
from .detection import (
    get_bounding_box_info,
    calculate_and_store_area,
    determine_direction,
    update_last_frame_boxes,
    is_surpassable,
    create_label,
)


def process_video(
    video_path: str | Path,
    model_path: str | Path,
    output_video_path: str | Path,
    class_indexes: list,
    classes: dict,
    box_annotator: sv.BoxAnnotator,
) -> None:
    """Process a video file and write the annotated output."""
    video_capture = cv2.VideoCapture(str(video_path))
    total_frames = int(video_capture.get(cv2.CAP_PROP_FRAME_COUNT))
    frame_width = int(video_capture.get(cv2.CAP_PROP_FRAME_WIDTH))
    frame_height = int(video_capture.get(cv2.CAP_PROP_FRAME_HEIGHT))
    fps = video_capture.get(cv2.CAP_PROP_FPS)
    fourcc = cv2.VideoWriter_fourcc(*"mp4v")
    output_video = cv2.VideoWriter(str(output_video_path), fourcc, fps, (frame_width, frame_height))

    model = ultralytics.YOLO(str(model_path))
    pbar = tqdm(total=total_frames, desc="Rendering video")

    past_areas: dict[int, list[float]] = {}
    last_frame_boxes: dict[int, list[float]] = {}
    N = 10

    while video_capture.isOpened():
        ret, frame = video_capture.read()
        if not ret:
            break
        result = model.predict(frame, classes=class_indexes, verbose=False, agnostic_nms=True)[0]
        detections = sv.Detections.from_yolov8(result)
        detections = detections[np.isin(detections.class_id, class_indexes)]
        new_labels = []

        for det in detections:
            xyxy, pixel_width, box_area, class_id, tracker_id = get_bounding_box_info(det)
            distance = calculate_distance(pixel_width)
            past_areas = calculate_and_store_area(past_areas, tracker_id, box_area, N)
            direction = determine_direction(past_areas, tracker_id, N, box_area)
            last_frame_boxes = update_last_frame_boxes(last_frame_boxes, tracker_id, xyxy)
            surpassable = is_surpassable(distance, direction)
            new_labels.append(create_label(classes, class_id, direction, distance, surpassable))

        frame = box_annotator.annotate(scene=frame, detections=detections, labels=new_labels)
        output_video.write(frame)
        pbar.update(1)

    pbar.close()
    video_capture.release()
    output_video.release()
    cv2.destroyAllWindows()
