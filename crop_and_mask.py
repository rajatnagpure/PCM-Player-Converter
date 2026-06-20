from PIL import Image, ImageDraw

def mask_rounded_square(input_path, output_path, crop_box, radius):
    # crop_box is (left, top, right, bottom)
    img = Image.open(input_path).convert("RGBA")
    img = img.crop(crop_box)
    
    # Create mask
    mask = Image.new('L', img.size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle((0, 0, img.size[0], img.size[1]), radius=radius, fill=255)
    
    # Apply mask
    img.putalpha(mask)
    
    # Save
    img.save(output_path, "PNG")
    print(f"Saved cropped and masked logo to {output_path}")

if __name__ == "__main__":
    input_file = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/logo_dark_neu_violet_1781946593877.png"
    output_file = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_final.png"
    
    # A 512x512 box centered in 1024x1024
    crop_box = (256, 256, 768, 768)
    radius = 110
    
    mask_rounded_square(input_file, output_file, crop_box, radius)
