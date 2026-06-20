import colorsys
from PIL import Image

def process():
    input_path = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_final.png"
    output_path_alpha = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_final_blue.png"
    output_path_black = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_black_bg.png"
    
    img = Image.open(input_path).convert("RGBA")
    pixels = img.load()
    width, height = img.size
    
    # Target color: #007FFF -> RGB(0, 127, 255)
    # Hue: 210 degrees / 360 = 0.5833
    target_h = 0.5833
    
    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a > 0:
                h, l, s = colorsys.rgb_to_hls(r/255.0, g/255.0, b/255.0)
                
                # Purple is around hue 260-300 degrees
                # So hue is between 0.65 and 0.90
                # We also want to capture pinkish hues, maybe up to 0.95
                if 0.60 <= h <= 0.95 and s > 0.05:
                    # Shift hue to Azure blue
                    h = target_h
                    # Boost saturation
                    s = min(1.0, s * 1.5)
                    
                    nr, ng, nb = colorsys.hls_to_rgb(h, l, s)
                    pixels[x, y] = (int(nr*255), int(ng*255), int(nb*255), a)
                    
    img.save(output_path_alpha, "PNG")
    
    # Create black background version
    bg = Image.new("RGBA", img.size, (0, 0, 0, 255))
    bg.paste(img, (0, 0), img)
    bg.save(output_path_black, "PNG")
    print("Color changed and files saved successfully.")

if __name__ == "__main__":
    process()
