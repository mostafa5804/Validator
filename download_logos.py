import json
import urllib.request
import os

banks = [
  {
    "bankName": "آینده",
    "shabaCode": "62",
    "cardPrefix": "636214",
    "logoUrl": "https://static.idpay.ir/banks/ayandeh.png"
  },
  {
    "bankName": "اقتصاد نوین",
    "shabaCode": "55",
    "cardPrefix": "627412",
    "logoUrl": "https://static.idpay.ir/banks/eghtesad-novin.png"
  },
  {
    "bankName": "ایران زمین",
    "shabaCode": "69",
    "cardPrefix": "505785",
    "logoUrl": "https://static.idpay.ir/banks/iran-zamin.png"
  },
  {
    "bankName": "پارسیان",
    "shabaCode": "54",
    "cardPrefix": "622106",
    "logoUrl": "https://static.idpay.ir/banks/parsian.png"
  },
  {
    "bankName": "پاسارگاد",
    "shabaCode": "57",
    "cardPrefix": "502229",
    "logoUrl": "https://static.idpay.ir/banks/pasargad.png"
  },
  {
    "bankName": "تجارت",
    "shabaCode": "18",
    "cardPrefix": "627353",
    "logoUrl": "https://static.idpay.ir/banks/tejarat.png"
  },
  {
    "bankName": "توسعه تعاون",
    "shabaCode": "22",
    "cardPrefix": "502908",
    "logoUrl": "https://static.idpay.ir/banks/tosee-taavon.png"
  },
  {
    "bankName": "توسعه صادرات ایران",
    "shabaCode": "20",
    "cardPrefix": "207177",
    "logoUrl": "https://static.idpay.ir/banks/export-development.png"
  },
  {
    "bankName": "خاورمیانه",
    "shabaCode": "80",
    "cardPrefix": "585983",
    "logoUrl": "https://static.idpay.ir/banks/middle-east.png"
  },
  {
    "bankName": "دی",
    "shabaCode": "66",
    "cardPrefix": "502938",
    "logoUrl": "https://static.idpay.ir/banks/dey.png"
  },
  {
    "bankName": "رفاه کارگران",
    "shabaCode": "13",
    "cardPrefix": "589463",
    "logoUrl": "https://static.idpay.ir/banks/refah.png"
  },
  {
    "bankName": "سامان",
    "shabaCode": "56",
    "cardPrefix": "621986",
    "logoUrl": "https://static.idpay.ir/banks/saman.png"
  },
  {
    "bankName": "سپه",
    "shabaCode": "15",
    "cardPrefix": "589210",
    "logoUrl": "https://static.idpay.ir/banks/sepah.png"
  },
  {
    "bankName": "سرمایه",
    "shabaCode": "58",
    "cardPrefix": "639607",
    "logoUrl": "https://static.idpay.ir/banks/sarmayeh.png"
  },
  {
    "bankName": "سینا",
    "shabaCode": "59",
    "cardPrefix": "639346",
    "logoUrl": "https://static.idpay.ir/banks/sina.png"
  },
  {
    "bankName": "شهر",
    "shabaCode": "61",
    "cardPrefix": "502806",
    "logoUrl": "https://static.idpay.ir/banks/shahr.png"
  },
  {
    "bankName": "صادرات ایران",
    "shabaCode": "19",
    "cardPrefix": "603769",
    "logoUrl": "https://static.idpay.ir/banks/saderat.png"
  },
  {
    "bankName": "صنعت و معدن",
    "shabaCode": "11",
    "cardPrefix": "627961",
    "logoUrl": "https://static.idpay.ir/banks/industry-and-mine.png"
  },
  {
    "bankName": "قرض الحسنه رسالت",
    "shabaCode": "70",
    "cardPrefix": "504172",
    "logoUrl": "https://static.idpay.ir/banks/resalat.png"
  },
  {
    "bankName": "قرض الحسنه مهر ایران",
    "shabaCode": "60",
    "cardPrefix": "606373",
    "logoUrl": "https://static.idpay.ir/banks/mehr-iran.png"
  },
  {
    "bankName": "کارآفرین",
    "shabaCode": "53",
    "cardPrefix": "627488",
    "logoUrl": "https://static.idpay.ir/banks/karafarin.png"
  },
  {
    "bankName": "کشاورزی",
    "shabaCode": "16",
    "cardPrefix": "603770",
    "logoUrl": "https://static.idpay.ir/banks/keshavarzi.png"
  },
  {
    "bankName": "گردشگری",
    "shabaCode": "64",
    "cardPrefix": "505416",
    "logoUrl": "https://static.idpay.ir/banks/tourism.png"
  },
  {
    "bankName": "مسکن",
    "shabaCode": "14",
    "cardPrefix": "628023",
    "logoUrl": "https://static.idpay.ir/banks/maskan.png"
  },
  {
    "bankName": "ملت",
    "shabaCode": "12",
    "cardPrefix": "610433",
    "logoUrl": "https://static.idpay.ir/banks/mellat.png"
  },
  {
    "bankName": "ملی ایران",
    "shabaCode": "17",
    "cardPrefix": "603799",
    "logoUrl": "https://static.idpay.ir/banks/melli.png"
  },
  {
    "bankName": "پست بانک ایران",
    "shabaCode": "21",
    "cardPrefix": "627760",
    "logoUrl": "https://static.idpay.ir/banks/post-bank.png"
  },
  {
    "bankName": "موسسه اعتباری ملل",
    "shabaCode": "75",
    "cardPrefix": "606256",
    "logoUrl": "https://static.idpay.ir/banks/melal.png"
  },
  {
    "bankName": "موسسه اعتباری نور",
    "shabaCode": "75",
    "cardPrefix": "207177",
    "logoUrl": "https://static.idpay.ir/banks/noor.png"
  }
]

drawable_dir = "app/src/main/res/drawable"
os.makedirs(drawable_dir, exist_ok=True)

for bank in banks:
    url = bank["logoUrl"]
    filename = url.split("/")[-1]
    name_without_ext = filename.split(".")[0].replace("-", "_")
    filepath = os.path.join(drawable_dir, f"ic_bank_{name_without_ext}.png")
    
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response, open(filepath, 'wb') as out_file:
            data = response.read()
            out_file.write(data)
            print(f"Downloaded {filename} to ic_bank_{name_without_ext}.png")
    except Exception as e:
        print(f"Failed to download {url}: {e}")

