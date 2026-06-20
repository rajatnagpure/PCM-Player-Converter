import cv2
import numpy as np
from PIL import Image

def process():
    input_path = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_final.png"
    output_path_alpha = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_final_blue.png"
    output_path_black = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_black_bg.png" # Overwrite black bg
    
    # Read with alpha
    img = cv2.imread(input_path, cv2.IMREAD_UNCHANGED)
    
    # Split channels
    b, g, r, a = cv2.split(img)
    bgr = cv2.merge((b,g,r))
    
    # Convert to HSV
    hsv = cv2.cvtColor(bgr, cv2.COLOR_BGR2HSV).astype(np.float32)
    
    h, s, v = cv2.split(hsv)
    
    # Target color: #007FFF
    # R=0, G=127, B=255
    target_bgr = np.uint8([[[255, 127, 0]]])
    target_hsv = cv2.cvtColor(target_bgr, cv2.COLOR_BGR2HSV)[0][0]
    target_h = target_hsv[0]
    
    # Find purple hue
    mask = (h > 110) & (h < 170) & (s > 10)
    
    # Shift hue
    h[mask] = target_h
    
    # Boost saturation slightly to match the bright #007FFF
    s[mask] = np.clip(s[mask] * 1.5, 0, 255)
    
    hsv_new = cv2.merge((h, s, v)).astype(np.uint8)
    bgr_new = cv2.cvtColor(hsv_new, cv2.COLOR_HSV2BGR)
    
    # Re-attach alpha
    b_new, g_new, r_new = cv2.split(bgr_new)
    rgba_new = cv2.merge((b_new, g_new, r_new, a))
    
    # Save the transparent one
    cv2.imwrite(output_path_alpha, rgba_new)
    
    # Create black background version using PIL
    pil_img = Image.fromarray(cv2.cvtColor(rgba_new, cv2.COLOR_BGRA2RGBA))
    bg = Image.new("RGBA", pil_img.size, (0, 0, 0, 255))
    bg.paste(pil_img, (0, 0), pil_img)
    bg.save(output_path_black, "PNG")

if __name__ == "__main__":
    process()
