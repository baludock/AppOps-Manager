import os
from PIL import Image

image_path = "/home/dos/Downloads/settings.png"
if not os.path.exists(image_path):
    image_path = "/home/dos/newapp/app/src/main/res/mipmap-xxxhdpi/ic_launcher.png"
base_dir = "/home/dos/newapp/app/src/main/res"

sizes = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

if not os.path.exists(image_path):
    print(f"Error: Could not find image at {image_path}")
    exit(1)

# Open user's image
user_img = Image.open(image_path).convert("RGBA")

# Crop any transparent padding
bbox = user_img.getbbox()
if bbox:
    user_img = user_img.crop(bbox)

# Ensure it fits nicely inside the bounds (leave some padding, e.g., 80% of max size)
# For the composite, let's make a base canvas of 512x512
canvas_size = 512
bg_image = Image.new("RGBA", (canvas_size, canvas_size), (0, 0, 0, 255)) # Pure black

# Remove padding, fill entire canvas
target_size = canvas_size

# Preserve aspect ratio or fit directly
user_img = user_img.resize((target_size, target_size), Image.Resampling.LANCZOS)

# Paste user image onto the black background exactly filling it
bg_image.paste(user_img, (0, 0), user_img)

# Now iterate and resize this final composite image for all buckets
for folder, size in sizes.items():
    folder_path = os.path.join(base_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    
    resized_img = bg_image.resize((size, size), Image.Resampling.LANCZOS)
    
    output_path = os.path.join(folder_path, "ic_launcher.png")
    round_output_path = os.path.join(folder_path, "ic_launcher_round.png")
    
    resized_img.save(output_path, "PNG")
    resized_img.save(round_output_path, "PNG")
    print(f"Saved {output_path} and {round_output_path}")

print("Successfully generated all mipmap icons with pure black background.")
