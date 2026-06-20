import os
from PIL import Image

def generate_icons(source_image_path, res_dir):
    # Standard Android launcher icon sizes
    sizes = {
        'mipmap-mdpi': 48,
        'mipmap-hdpi': 72,
        'mipmap-xhdpi': 96,
        'mipmap-xxhdpi': 144,
        'mipmap-xxxhdpi': 192,
        'playstore': 512
    }

    try:
        img = Image.open(source_image_path)
    except Exception as e:
        print(f"Error opening image {source_image_path}: {e}")
        return

    # Ensure it's RGBA for transparency
    img = img.convert("RGBA")

    for folder, size in sizes.items():
        resized_img = img.resize((size, size), Image.Resampling.LANCZOS)
        
        if folder == 'playstore':
            output_path = os.path.join(res_dir, '..', '..', 'playstore_icon.png')
            resized_img.save(output_path, 'PNG')
            print(f"Generated {folder} icon: {output_path}")
            continue
            
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        # Save ic_launcher.png
        output_path = os.path.join(folder_path, 'ic_launcher.png')
        resized_img.save(output_path, 'PNG')
        
        # Save ic_launcher_round.png
        output_path_round = os.path.join(folder_path, 'ic_launcher_round.png')
        resized_img.save(output_path_round, 'PNG')
        
        print(f"Generated {folder} icons.")

if __name__ == "__main__":
    # Source image from the conversation
    source_img = "/Users/rajatnagpure/.gemini/antigravity/brain/8554c2cd-9cc3-4f33-9341-2df894d43e36/processed_logo_black_bg.png"
    
    # Path to Android res folder
    res_dir = "/Users/rajatnagpure/StudioProjects/PCM-Player-Converter/app/src/main/res"
    
    generate_icons(source_img, res_dir)
