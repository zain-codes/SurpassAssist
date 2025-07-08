from typing import Dict, List, Tuple


def get_bounding_box_info(det) -> Tuple[List[float], float, float, int, int]:
    """Extract information from a detection result."""
    xyxy, mask, confidence, class_id, tracker_id = det
    pixel_width = xyxy[2] - xyxy[0]
    box_area = pixel_width * (xyxy[3] - xyxy[1])
    return xyxy, pixel_width, box_area, class_id, tracker_id


def calculate_and_store_area(
    past_areas: Dict[int, List[float]], tracker_id: int, box_area: float, N: int
) -> Dict[int, List[float]]:
    """Keep track of the most recent N bounding box areas."""
    past_areas.setdefault(tracker_id, []).append(box_area)
    past_areas[tracker_id] = past_areas[tracker_id][-N:]
    return past_areas


def determine_direction(
    past_areas: Dict[int, List[float]], tracker_id: int, N: int, box_area: float
) -> str:
    """Estimate whether the object is moving toward or away from the camera."""
    if len(past_areas[tracker_id]) == N:
        avg_past_area = sum(past_areas[tracker_id]) / N
        direction = "moving away" if box_area > avg_past_area else "Incoming"
    else:
        direction = "Direction unknown"
    return direction


def update_last_frame_boxes(
    last_frame_boxes: Dict[int, List[float]], tracker_id: int, xyxy: List[float]
) -> Dict[int, List[float]]:
    """Store bounding boxes from the previous frame."""
    last_frame_boxes[tracker_id] = xyxy
    return last_frame_boxes


def is_surpassable(distance: float, direction: str) -> bool:
    """Return True if overtaking is considered safe."""
    if distance < 70 and direction == "moving away":
        return False
    return True


def create_label(
    classes: dict, class_id: int, direction: str, distance: float, surpassable: bool
) -> str:
    """Format the annotation label."""
    state = "Safe" if surpassable else "Unsafe"
    return f"{direction}, Distance: {distance:.2f}m - ({state})"
