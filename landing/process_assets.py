from PIL import Image
import os

def process_all():
    # 1. ICON: Resize 1024 -> 512
    if os.path.exists('master_icon.png'):
        print("Processing Icon...")
        img = Image.open('master_icon.png')
        img = img.resize((512, 512), Image.Resampling.LANCZOS)
        img.save('kkaloai_icon_512.png', 'PNG')
        print("Done: kkaloai_icon_512.png")

    # 2. BANNER: Crop 1024x1024 -> 1024x500 (Center)
    if os.path.exists('master_banner.png'):
        print("Processing Banner...")
        img = Image.open('master_banner.png')
        width, height = img.size
        # Crop center 500px height
        top = (height - 500) // 2
        bottom = top + 500
        img = img.crop((0, top, width, bottom))
        img.save('kkaloai_feature_graphic.png', 'PNG')
        print("Done: kkaloai_feature_graphic.png")

    # 3. SCREENSHOTS: Crop 1024x1024 -> 576x1024 (9:16 Portrait Center)
    screenshots = [
        'kkaloai_ss1_scanner.png',
        'kkaloai_ss2_adjust.png',
        'kkaloai_ss3_ecosystem.png',
        'kkaloai_ss4_modes.png'
    ]
    
    for ss in screenshots:
        if os.path.exists(ss):
            print(f"Processing {ss}...")
            img = Image.open(ss)
            width, height = img.size # 1024, 1024
            # Target width for 9:16 when height is 1024: (9/16) * 1024 = 576
            target_w = 576
            left = (width - target_w) // 2
            right = left + target_w
            img = img.crop((left, 0, right, height))
            # Save to a new 'final' filename to be clean, or overwrite? 
            # I will overwrite to match user's previous filenames but 'fixed' versions
            img.save(ss.replace('.png', '_fixed.png'), 'PNG')
            print(f"Done: {ss.replace('.png', '_fixed.png')}")

if __name__ == '__main__':
    process_all()
