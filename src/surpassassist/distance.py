"""Utilities for distance estimation."""

FOCAL_LENGTH = 700  # pixel units, calibrate for your camera
ACTUAL_WIDTH = 1.95  # average width of a car in meters


def calculate_distance(pixel_width: float) -> float:
    """Compute the distance from the camera to the object."""
    return (ACTUAL_WIDTH * FOCAL_LENGTH) / pixel_width
