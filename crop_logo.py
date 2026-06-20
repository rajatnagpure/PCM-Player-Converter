import cv2
import numpy as np
from PIL import Image

def crop_to_square(input_path, output_path):
    # Load image in grayscale
    img = cv2.imread(input_path, cv2.IMREAD_UNCHANGED)
    
    if img is None:
        print(f"Failed to load image from {input_path}")
        return

    # If it has alpha channel, use it, or convert to grayscale
    if len(img.shape) == 3 and img.shape[2] == 4:
        # Create mask from alpha
        mask = img[:,:,3]
    else:
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        # Threshold to separate the square from background
        # Assume background is very bright (white) or very dark (black)
        # Let's try edge detection
        edges = cv2.Canny(gray, 50, 150)
        mask = cv2.dilate(edges, None, iterations=2)

    # Find contours
    contours, _ = cv2.findContours(mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    
    if not contours:
        print("No contours found")
        return

    # Find the largest contour which should be our rounded square
    largest_contour = max(contours, key=cv2.contourArea)
    
    # Get bounding box of the largest contour
    x, y, w, h = cv2.boundingRect(largest_contour)
    
    # Add a tiny bit of padding (like 2%) just to avoid clipping anti-aliased edges
    pad_w = int(w * 0.02)
    pad_h = int(h * 0.02)
    
    x1 = max(0, x - pad_w)
    y1 = max(0, y - pad_h)
    x2 = min(img.shape[1], x + w + pad_w)
    y2 = min(img.shape[0], y + h + pad_h)
    
    cropped = img[y1:y2, x1:x2]
    
    # Convert cropped to PIL image to ensure transparent background is preserved
    # if it doesn't have an alpha channel, we'll just save it as is
    cv2.imwrite(output_path, cropped)
    print(f"Cropped logo saved to {output_path}")

    # Now we need to remove the white corners outside the rounded square to make them transparent
    pil_img = Image.open(output_path).convert("RGBA")
    datas = pil_img.getdata()
    newData = []
    for item in datas:
        # If pixel is pure white or very close to it, make it transparent
        if item[0] > 240 and item[1] > 240 and item[2] > 240:
            newData.append((255, 255, 255, 0))
        else:
            newData.append(item)
    pil_img.putdata(newData)
    pil_img.save(output_path, "PNG")

if __name__ == "__main__":
    input_file = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/logo_dark_neu_violet_1781946593877.png"
    output_file = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo2.png"
    crop_to_square(input_file, output_file)
