"""Lane detection utilities using the UFLD model."""

from pathlib import Path

import cv2
import numpy as np
import torch
from torchvision import transforms


class LaneDetector:
    """Detect lane boundaries using a pre-trained UFLD network."""

    def __init__(self, weights_path: str | Path) -> None:
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        self.model = torch.hub.load(
            "cfzd/Ultra-Fast-Lane-Detection",
            "resnet18",
            pretrained=False,
            source="github",
        ).to(self.device)
        state = torch.load(weights_path, map_location=self.device)
        if isinstance(state, dict) and "model" in state:
            state = state["model"]
        self.model.load_state_dict(state)
        self.model.eval()
        self.input_width = 800
        self.input_height = 288
        self.transform = transforms.Compose(
            [
                transforms.ToTensor(),
                transforms.Normalize(
                    mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]
                ),
            ]
        )

    def detect(self, frame: np.ndarray) -> np.ndarray:
        """Return a binary mask where lane pixels are 1."""
        img = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        img = cv2.resize(img, (self.input_width, self.input_height))
        tensor = self.transform(img).unsqueeze(0).to(self.device)
        with torch.no_grad():
            out = self.model(tensor)[0]
        mask = out.argmax(dim=0).cpu().numpy().astype(np.uint8)
        mask = cv2.resize(
            mask, (frame.shape[1], frame.shape[0]), interpolation=cv2.INTER_NEAREST
        )
        return mask

    @staticmethod
    def is_left_lane_clear(mask: np.ndarray) -> bool:
        """Simple heuristic to see if the left side is free of lane markings."""
        height, width = mask.shape
        left_region = mask[:, : width // 3]
        return np.count_nonzero(left_region) == 0

    @staticmethod
    def overlay_lanes(frame: np.ndarray, mask: np.ndarray) -> np.ndarray:
        """Overlay detected lanes on the frame."""
        overlay = frame.copy()
        overlay[mask > 0] = (0, 255, 0)
        return cv2.addWeighted(frame, 0.7, overlay, 0.3, 0)
