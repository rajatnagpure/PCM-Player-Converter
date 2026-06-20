from PIL import Image

def add_black_bg():
    input_path = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_final.png"
    output_path = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_black_bg.png"
    
    fg = Image.open(input_path).convert("RGBA")
    
    # Create black background of same size
    bg = Image.new("RGBA", fg.size, (0, 0, 0, 255))
    
    # Paste foreground onto background using alpha channel of fg as mask
    bg.paste(fg, (0, 0), fg)
    
    # Save
    bg.save(output_path, "PNG")
    print(f"Saved black background logo to {output_path}")

if __name__ == "__main__":
    add_black_bg()
