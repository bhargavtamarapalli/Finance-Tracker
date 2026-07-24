# Python script to compile Google Stitch mockups into a single working SPA with desktop & mobile toggle capability

import os
import re
import json

base_dir = "/Users/bhargavtamarapalli/Softwares/Android/Finance Manager/Mockups/extracted"
out_dir = "/Users/bhargavtamarapalli/Softwares/Android/Finance Manager/admin-panel"
os.makedirs(out_dir, exist_ok=True)

# Define screen mappings
screens = {
    "dashboard": {
        "web_dir": "stitch_premium_finance_dashboard (2)",
        "mobile_dir": "stitch_premium_finance_dashboard (4)"
    },
    "users": {
        "web_dir": "stitch_premium_finance_dashboard",
        "mobile_dir": "stitch_premium_finance_dashboard (1)"
    },
    "analytics": {
        "web_dir": "stitch_premium_finance_dashboard (5)",
        "mobile_dir": "stitch_premium_finance_dashboard (6)"
    },
    "settings": {
        "web_dir": "stitch_premium_finance_dashboard (7)",
        "mobile_dir": "stitch_premium_finance_dashboard (8)"
    }
}

# Collect all custom CSS from styles
custom_styles = set()
style_pattern = re.compile(r"<style>(.*?)</style>", re.DOTALL)

for key, config in screens.items():
    for mode in ["web_dir", "mobile_dir"]:
        folder = config[mode]
        filepath = os.path.join(base_dir, folder, "code.html")
        if not os.path.exists(filepath):
            print(f"Error: {filepath} not found!")
            continue
        with open(filepath, "r") as f:
            content = f.read()
        
        # Find custom styles
        matches = style_pattern.findall(content)
        for match in matches:
            # Clean up styles
            style_content = match.strip()
            # Split into individual rules and add to set to deduplicate
            custom_styles.add(style_content)

merged_styles = "\n\n".join(sorted(list(custom_styles)))

# Extract body contents
screen_data = {}

body_pattern = re.compile(r"<body[^>]*>(.*?)</body>", re.DOTALL)
main_pattern = re.compile(r"<main[^>]*>(.*?)</main>", re.DOTALL)

for key, config in screens.items():
    screen_data[key] = {}
    for mode_key, mode_name in [("web_dir", "web"), ("mobile_dir", "mobile")]:
        folder = config[mode_key]
        filepath = os.path.join(base_dir, folder, "code.html")
        with open(filepath, "r") as f:
            content = f.read()
        
        # Extract body content
        body_match = body_pattern.search(content)
        body_html = body_match.group(1) if body_match else content
        
        if mode_name == "web":
            # For web view, we want to extract the inner content of <main> to swap it dynamically.
            # If there's no <main> tag, just use the entire body
            main_match = main_pattern.search(content)
            if main_match:
                # Keep everything inside main except the header (we will have a unified header)
                # But to preserve the look exactly as designed, let's keep the header inside the view
                web_html = main_match.group(1)
            else:
                web_html = body_html
            
            # Clean up the desktop sidebar from the web_html if it got included
            # (it shouldn't be inside <main> anyway since <aside> is siblings with <main> in the DOM)
            screen_data[key]["web"] = web_html.strip()
        else:
            screen_data[key]["mobile"] = body_html.strip()

# Now, write the JSON file containing the templates
templates_js_path = os.path.join(out_dir, "templates.js")
with open(templates_js_path, "w") as f:
    f.write("const templates = ")
    json.dump(screen_data, f, indent=2)
    f.write(";\n")

# Write the CSS file
styles_css_path = os.path.join(out_dir, "custom.css")
with open(styles_css_path, "w") as f:
    f.write(merged_styles)

print("Mockups compilation completed!")
