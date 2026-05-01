#!/usr/bin/env python3
"""Download and rename catalog images into app/src/main/res/drawable."""

from pathlib import Path
from urllib.request import urlopen

TARGET_DIR = Path("app/src/main/res/drawable")

IMAGE_MAP = {
    # LeisureVisualCatalog
    "leisure_hk_museum_of_art.jpg": "https://upload.wikimedia.org/wikipedia/commons/4/4e/Hong_Kong_Museum_of_Art_20250716.jpg",
    "leisure_hk_museum_of_history.jpg": "https://upload.wikimedia.org/wikipedia/commons/1/1d/HK_Hong_Kong_Museum_of_History.JPG",
    "leisure_hk_science_museum.jpg": "https://upload.wikimedia.org/wikipedia/commons/f/ff/HKScienceMuseumview.jpg",
    "leisure_hk_space_museum.jpg": "https://upload.wikimedia.org/wikipedia/commons/2/26/Hong_Kong_Space_Museum.jpg",
    "leisure_hk_heritage_museum.jpg": "https://upload.wikimedia.org/wikipedia/commons/5/5f/Hong_Kong_Heritage_Museum_201305.jpg",
    "leisure_flagstaff_house.jpg": "https://upload.wikimedia.org/wikipedia/commons/3/3e/Flagstaff_House%2C_Museum_of_Tea_Ware.JPG",
    "leisure_dr_sun_yat_sen_museum.jpg": "https://upload.wikimedia.org/wikipedia/commons/0/03/Dr_Sun_Yat-sen_Museum.jpg",
    "leisure_hk_visual_arts_centre.jpg": "https://upload.wikimedia.org/wikipedia/commons/9/9c/Hong_Kong_Visual_Arts_Center.JPG",
    "leisure_hk_film_archive.jpg": "https://upload.wikimedia.org/wikipedia/commons/f/fd/Hong_Kong_Film_Archive01.jpg",
    "leisure_tai_kwun.jpg": "https://upload.wikimedia.org/wikipedia/commons/1/13/Tai_Kwun_Parade_Ground_201806.jpg",
    "leisure_west_kowloon.jpg": "https://upload.wikimedia.org/wikipedia/commons/e/ed/M%2B%2C_West_Kowloon_Cultural_District_%28Hong_Kong%29.jpg",
    "leisure_central_harbourfront.jpg": "https://upload.wikimedia.org/wikipedia/commons/f/f5/Central_Harbourfront_Event_Space%2C_Hong_Kong.jpg",
    "leisure_victoria_harbour.jpg": "https://upload.wikimedia.org/wikipedia/commons/0/01/Victoria_Harbour_%28from_Lugard_Road%29.jpg",
    "leisure_hkcec.jpg": "https://upload.wikimedia.org/wikipedia/commons/4/4d/HKCEC.jpg",
    "leisure_kai_tak.jpg": "https://upload.wikimedia.org/wikipedia/commons/6/66/Kai_Tak_Sports_Park_2025.jpg",
    "leisure_harbour_city.jpg": "https://upload.wikimedia.org/wikipedia/commons/e/e9/Harbour_City_Front.JPG",
    # PlaceProfileCatalog
    "hospital_queen_mary.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3d/Queen_Mary_Hospital_25-11-2023.jpg/1200px-Queen_Mary_Hospital_25-11-2023.jpg",
    "hospital_queen_elizabeth.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e3/HK_King%27s_Park_%E4%BC%8A%E5%88%A9%E6%B2%99%E4%BC%AF%E9%86%AB%E9%99%A2_Queen_Elizabeth_Hospital_outdoor_entrance_Jan-2014.JPG/1200px-HK_King%27s_Park_%E4%BC%8A%E5%88%A9%E6%B2%99%E4%BC%AF%E9%86%AB%E9%99%A2_Queen_Elizabeth_Hospital_outdoor_entrance_Jan-2014.JPG",
    "hospital_prince_of_wales.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/4/40/Prince_of_Wales_Hospital_Overview_201106.jpg/1200px-Prince_of_Wales_Hospital_Overview_201106.jpg",
    "hospital_pamela_youde_eastern.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/5/50/Pamela_Youde_Nethersole_Eastern_Hospital_%28A%26E%29.JPG/1200px-Pamela_Youde_Nethersole_Eastern_Hospital_%28A%26E%29.JPG",
    "hospital_princess_margaret.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c5/Princess_Margaret_Hospital_202101.jpg/1200px-Princess_Margaret_Hospital_202101.jpg",
    "hospital_north_district.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d4/North_District_Hospital_New_Acute_Block_-_February_2026.jpg/1200px-North_District_Hospital_New_Acute_Block_-_February_2026.jpg",
    "hospital_tseung_kwan_o.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/36/Tseung_Kwan_O_Hospital.jpg/1200px-Tseung_Kwan_O_Hospital.jpg",
    "hospital_tuen_mun.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/d/d3/Tuen_Mun_Hospital_%28full_view%29.jpg/1200px-Tuen_Mun_Hospital_%28full_view%29.jpg",
    "hospital_united_christian.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/8/84/United_Christian_Hospital_2021.jpg/1200px-United_Christian_Hospital_2021.jpg",
    "hospital_st_john.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2e/St._John_Hospital%2C_Cheung_Chau_2026.jpg/1200px-St._John_Hospital%2C_Cheung_Chau_2026.jpg",
    "parking_central.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/2/2a/HK_Central_Edinburgh_Place_Star_Ferry_Carpark_Building_view_Jardine_House_facade_May-2012.JPG/1200px-HK_Central_Edinburgh_Place_Star_Ferry_Carpark_Building_view_Jardine_House_facade_May-2012.JPG",
    "parking_kai_tak.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/3/3a/Kai_Tak_Cruise_Terminal_carpark_09-04-2016%281%29.jpg/1200px-Kai_Tak_Cruise_Terminal_carpark_09-04-2016%281%29.jpg",
    "parking_district.jpg": "https://upload.wikimedia.org/wikipedia/commons/thumb/a/ab/HK_Tsuen_Wan_Town_Hall_%E8%8D%83%E7%81%A3%E5%A4%A7%E6%9C%83%E5%A0%82_outdoor_carpark_red_flagpoles_view_Vision_City_facade_May-2013.JPG/1200px-HK_Tsuen_Wan_Town_Hall_%E8%8D%83%E7%81%A3%E5%A4%A7%E6%9C%83%E5%A0%82_outdoor_carpark_red_flagpoles_view_Vision_City_facade_May-2013.JPG",
}


def main() -> None:
    TARGET_DIR.mkdir(parents=True, exist_ok=True)
    for filename, url in IMAGE_MAP.items():
        output = TARGET_DIR / filename
        with urlopen(url) as r:
            data = r.read()
        output.write_bytes(data)
        print(f"Downloaded {filename} ({len(data)} bytes)")


if __name__ == "__main__":
    main()
