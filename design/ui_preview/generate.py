from PIL import Image, ImageDraw, ImageFont
from pathlib import Path
import math

OUT=Path(__file__).parent
W,H=390,844
BG='#0B1224'; CARD='#15213A'; CARD2='#1B2B47'; LINE='#2B3A57'; WHITE='#F6F9FF'; MUTED='#9EACC5'; CYAN='#44DED8'; PURPLE='#A78BFA'; GREEN='#70E5B1'; ORANGE='#FFBD78'
FONT='/System/Library/Fonts/Avenir Next.ttc'
def font(n,b=False): return ImageFont.truetype(FONT,n,index=0 if b else 7)
def txt(d,xy,s,n=16,c=WHITE,b=False): d.text(xy,s,font=font(n,b),fill=c)
def rr(d,box,r=18,fill=CARD,outline=None,width=1): d.rounded_rectangle(box,radius=r,fill=fill,outline=outline,width=width)
def line(d,xy,fill=LINE,w=1):d.line(xy,fill=fill,width=w)
def dot(d,x,y,r,c):d.ellipse((x-r,y-r,x+r,y+r),fill=c)
def button(d,y,label='Continue',c=CYAN):
    rr(d,(24,y,366,y+52),18,c); txt(d,(195-d.textlength(label,font=font(16,True))/2,y+14),label,16,BG,True)
def icon(d,x,y,s,c=CYAN):
    rr(d,(x,y,x+40,y+40),12,CARD2)
    rr(d,(x+12,y+11,x+28,y+27),5,None,c,2)
    line(d,(x+16,y+31,x+24,y+31),c,2)
def row(d,y,s,sub='',value='',symbol='›',c=CYAN):
    rr(d,(24,y,366,y+67),16,CARD)
    dot(d,49,y+33,14,CARD2);dot(d,49,y+33,5,c)
    txt(d,(75,y+12),s,15,WHITE,True)
    if sub:txt(d,(75,y+36),sub,11,MUTED)
    if value:txt(d,(318-d.textlength(value,font=font(12)),y+25),value,12,MUTED)
    txt(d,(341,y+19),'›',22,MUTED)
def base(title,kicker=None,tab=False):
    im=Image.new('RGB',(W,H),BG);d=ImageDraw.Draw(im)
    txt(d,(24,16),'11:08',13,MUTED,True);txt(d,(322,16),'●  100%',12,MUTED)
    if kicker:txt(d,(24,62),kicker.upper(),11,CYAN,True)
    txt(d,(24,82 if kicker else 64),title,29,WHITE,True)
    if not tab:txt(d,(344,69),'×',23,MUTED)
    if tab:
        rr(d,(0,774,390,844),0,'#111B30');line(d,(0,774,390,774))
        for i,(s,g) in enumerate([('H','Home'),('S','Swipe'),('C','Compress'),('T','Tools')]):
            x=24+i*94;col=CYAN if title.lower().startswith(g.lower()) else MUTED
            txt(d,(x+20,786),s,23,col,True);txt(d,(x+12,817),g,11,col)
    return im,d
def hero(d,label,value,sub,cta):
    rr(d,(24,140,366,355),27,CARD,LINE)
    d.arc((46,165,176,295),-90,200,fill='#34445E',width=13);d.arc((46,165,176,295),-90,65,fill=CYAN,width=13)
    txt(d,(72,210),value,25,WHITE,True);txt(d,(194,178),label,17,WHITE,True)
    txt(d,(194,207),sub,12,MUTED)
    rr(d,(46,300,344,336),13,CYAN);txt(d,(195-d.textlength(cta,font=font(14,True))/2,308),cta,14,BG,True)
def section(d,y,title,caption=''):
    txt(d,(24,y),title,19,WHITE,True)
    if caption:txt(d,(24,y+27),caption,12,MUTED)
def tiles(d,y,items):
    for i,(name,value,sym) in enumerate(items):
        x=24+(i%2)*176; yy=y+(i//2)*102
        rr(d,(x,yy,x+166,yy+90),19,CARD)
        icon(d,x+12,yy+12,sym,CYAN if i%3==0 else PURPLE)
        txt(d,(x+59,yy+15),name,13,WHITE,True)
        txt(d,(x+59,yy+39),value,11,MUTED)
def listpage(title,sub,items,action='Clean selected',badge='3.8 GB'):
    im,d=base(title,'Storage cleanup');txt(d,(24,125),sub,13,MUTED)
    rr(d,(24,162,366,228),19,CARD2);txt(d,(42,176),'AVAILABLE TO REVIEW',11,CYAN,True);txt(d,(42,196),badge,24,WHITE,True)
    txt(d,(24,251),'Select all',14,WHITE,True);txt(d,(326,251),'○',19,CYAN)
    for i,(a,b) in enumerate(items[:6]):row(d,285+i*75,a,b,symbol='□')
    button(d,760,action)
    return im
pages={}
im,d=base('Home','Your storage, in control',True);hero(d,'Storage health','68%','72.4 GB of 128 GB used','Start smart clean');section(d,379,'Recommended','A focused plan for your device');tiles(d,425,[('Similar photos','246 items','▧'),('Large files','3.8 GB','▤'),('Screenshots','82 items','▣'),('Cache cleanup','1.2 GB','✦')]);section(d,654,'Quick tools');row(d,687,'Private vault','Keep personal files protected');pages['01_home']=im
im,d=base('Swipe','Photo cleanup',True);txt(d,(24,132),'Clear clutter one moment at a time.',13,MUTED);rr(d,(24,177,366,536),26,CARD);rr(d,(47,199,343,480),21,'#243957');d.ellipse((120,250,270,400),fill='#425F75');txt(d,(104,320),'PHOTO PREVIEW',18,WHITE,True);txt(d,(86,496),'August 2026  ·  128 photos',15,WHITE,True);txt(d,(74,566),'← Keep',18,GREEN,True);txt(d,(259,566),'Delete →',18,ORANGE,True);row(d,635,'Review by month','Continue your cleanup','', '▧');pages['02_swipe']=im
im,d=base('Compress','Make room for more',True);section(d,145,'Choose what to compress','Keep the moments. Reduce the size.');tiles(d,205,[('Video','Save up to 60%','▷'),('Photos','Smaller, still sharp','▣')]);rr(d,(24,430,366,631),23,CARD);txt(d,(45,454),'BEFORE',12,MUTED,True);txt(d,(211,454),'AFTER',12,CYAN,True);rr(d,(43,484,180,592),14,CARD2);rr(d,(210,484,347,592),14,'#1A3B4B');txt(d,(77,523),'24 MB',17,WHITE,True);txt(d,(241,523),'9 MB',17,CYAN,True);txt(d,(24,669),'Quality stays in your hands',15,WHITE,True);txt(d,(24,698),'Review the result before saving.',12,MUTED);pages['03_compress']=im
im,d=base('Tools','Everything in one place',True);section(d,145,'Cleanup');tiles(d,189,[('Smart clean','Scan device','✦'),('App manager','Review apps','▦'),('Large files','Find space','▤'),('Contacts','Merge copies','◎')]);section(d,413,'Utilities');tiles(d,455,[('Private vault','Secure files','◇'),('Image enhance','Improve photos','✧'),('Speed test','Check network','↗'),('Calendar','Remove clutter','▧')]);pages['04_tools']=im
im,d=base('Smart clean','Device scan');hero(d,'Ready to reclaim','4.6 GB','Across 312 items','Review cleanup');section(d,383,'What we found');tiles(d,424,[('Photos','1.8 GB','▧'),('Videos','1.4 GB','▷'),('Cache','1.2 GB','✦'),('Contacts','12 copies','◎')]);pages['05_smart_clean']=im
categories=[('Similar photos','Keep your best shots',[('Beach trip','12 similar photos'),('Family moments','8 similar photos'),('City walk','6 similar photos')]),('Duplicate photos','Exact copies ready to review',[('IMG_2048','4 copies'),('Holiday album','3 copies'),('Camera roll','2 copies')]),('Screenshots','Review old captures',[('Today','14 screenshots'),('This week','28 screenshots'),('Older','40 screenshots')]),('Duplicate videos','Remove repeated clips',[('Travel montage','3 copies'),('Birthday clip','2 copies'),('Screen recording','2 copies')]),('All videos','Browse and organize videos',[('Summer trip','1.2 GB'),('Screen recordings','860 MB'),('Downloads','520 MB')]),('Large files','Big files, clear choices',[('4K holiday video','1.6 GB'),('Offline movie','980 MB'),('Archive backup','740 MB')]),('Cache cleanup','Temporary files',[('Gallery cache','420 MB'),('Social apps','380 MB'),('Browser cache','210 MB')]),('Contacts cleanup','Find and merge copies',[('Alex Morgan','3 contacts'),('Jamie Lee','2 contacts'),('Sam Rivera','2 contacts')]),('App manager','Manage your space',[('Video editor','1.3 GB'),('Social app','820 MB'),('Travel maps','640 MB')]),('Picture cleanup','Photos to review',[('Camera','1,420 photos'),('Downloads','186 photos'),('Edited','94 photos')]),('Video cleanup','Videos to review',[('Camera','34 videos'),('Downloads','12 videos'),('Screen records','8 videos')]),('Audio cleanup','Audio to review',[('Music','228 files'),('Recordings','42 files'),('Downloads','19 files')])]
for j,(title,sub,items) in enumerate(categories,6):pages[f'{j:02d}_{title.lower().replace(" ","_")}']=listpage(title,sub,items,'Review selected', '2.4 GB' if 'video' in title.lower() else '846 MB')
im,d=base('Photo review','August 2026');rr(d,(24,148,366,570),26,CARD);rr(d,(44,169,346,542),18,'#344F68');d.ellipse((96,240,282,426),fill='#547C86');txt(d,(117,348),'PHOTO',24,WHITE,True);txt(d,(24,604),'42 of 128',14,MUTED);txt(d,(291,604),'♡  Save',14,GREEN,True);button(d,694,'Move to trash',ORANGE);pages['18_photo_review']=im
im,d=base('Video compress','Optimize a video');rr(d,(24,148,366,378),24,CARD);txt(d,(170,228),'PLAY',24,CYAN,True);txt(d,(44,337),'Summer trip.mp4',16,WHITE,True);section(d,412,'Compression level');row(d,456,'Balanced','Best quality and size','Recommended','✓');row(d,537,'Maximum saving','Smaller file size','','○');row(d,618,'Original quality','Light compression','','○');button(d,760,'Compress video');pages['19_video_compress']=im
im,d=base('Private vault','Protected files');rr(d,(24,151,366,363),24,CARD);txt(d,(155,213),'LOCK',25,CYAN,True);txt(d,(102,290),'Your files, private',21,WHITE,True);txt(d,(73,326),'Unlock to view protected items',12,MUTED);button(d,396,'Unlock vault');section(d,515,'Private by design');row(d,558,'Photos and videos','Stored behind your device lock');pages['20_private_vault']=im
im,d=base('Image enhance','Photo tools');rr(d,(24,150,366,492),24,CARD);rr(d,(46,173,344,466),16,'#27475A');txt(d,(120,304),'ENHANCE',22,WHITE,True);section(d,523,'Bring out the details');txt(d,(24,557),'Sharper, brighter photos in a few taps.',13,MUTED);button(d,657,'Choose photo');pages['21_image_enhance']=im
im,d=base('Speed test','Network tools');hero(d,'Download speed','84','Mbps · Strong connection','Run test again');section(d,405,'Connection details');row(d,450,'Upload','38 Mbps','', '↗');row(d,529,'Latency','19 ms','', '◉');pages['22_speed_test']=im
im,d=base('Network traffic','Network tools');rr(d,(24,151,366,381),23,CARD);txt(d,(45,174),'DATA USED THIS MONTH',11,CYAN,True);txt(d,(45,204),'12.8 GB',34,WHITE,True);forbars=[64,84,101,79,118,93,132];
for i,h in enumerate(forbars):rr(d,(49+i*43,347-h,70+i*43,347),7,CYAN if i==6 else '#365B75')
section(d,414,'Usage by app');row(d,455,'Streaming','5.2 GB');row(d,534,'Social','3.1 GB');row(d,613,'Browser','1.6 GB');pages['23_network_traffic']=im
im,d=base('Battery info','Device insights');hero(d,'Battery health','92%','Charging · 2h to full','View details');section(d,406,'At a glance');row(d,449,'Temperature','Normal','32°C','◉');row(d,528,'Capacity','Estimated','4,200 mAh','▤');pages['24_battery_info']=im
im,d=base('Device info','Device insights');rr(d,(24,149,366,291),23,CARD);txt(d,(46,174),'DEVICE',11,CYAN,True);txt(d,(46,203),'Galaxy device',23,WHITE,True);txt(d,(46,245),'Android  •  128 GB storage',12,MUTED);section(d,326,'System overview');row(d,367,'Storage','72.4 GB used','56%','▤');row(d,446,'Memory','4.8 GB available','','▦');row(d,525,'Operating system','Android','','◇');pages['25_device_info']=im
im,d=base('Calendar cleanup','Organize');section(d,148,'Old events','Review entries before removing');row(d,204,'Past reminders','42 events','','▧');row(d,284,'Duplicate events','8 events','','▧');row(d,364,'Imported calendars','3 calendars','','▧');button(d,760,'Review events');pages['26_calendar_cleanup']=im
im,d=base('Cleanup complete','Your space, refreshed');rr(d,(24,168,366,484),27,CARD);dot(d,195,248,55,'#1D685F');txt(d,(164,232),'OK',30,CYAN,True);txt(d,(102,320),'2.4 GB saved',29,WHITE,True);txt(d,(97,365),'128 items cleaned safely',14,MUTED);button(d,548,'Back to home');pages['27_cleanup_result']=im
im,d=base('Settings','Preferences');row(d,146,'Language','Choose your language','','◎');row(d,225,'Notifications','Useful storage reminders','','◉');row(d,304,'Privacy policy','How your data is handled','','◇');row(d,383,'Terms of use','Product information','','▤');txt(d,(151,721),'Version 1.0',12,MUTED);pages['28_settings']=im
im,d=base('Language','Preferences');
for i,(name,sub) in enumerate([('English','Selected'),('简体中文','Chinese'),('Español','Spanish'),('Français','French'),('Português','Portuguese')]):row(d,152+i*78,name,sub,'✓' if i==0 else '', '◎')
pages['29_language']=im
im,d=base('Privacy policy','Information');rr(d,(24,150,366,699),22,CARD);txt(d,(44,177),'Your privacy matters',22,WHITE,True);txt(d,(44,222),'Your files stay on your device.',14,MUTED);txt(d,(44,256),'Review permissions and data use',14,MUTED);txt(d,(44,290),'in the full policy.',14,MUTED);pages['30_privacy']=im
im,d=base('Terms of use','Information');rr(d,(24,150,366,699),22,CARD);txt(d,(44,177),'Using the app',22,WHITE,True);txt(d,(44,222),'Clear terms, readable sections,',14,MUTED);txt(d,(44,256),'and accessible links.',14,MUTED);pages['31_terms']=im
im,d=base('Media preview','Review file');rr(d,(24,149,366,609),24,CARD);rr(d,(42,167,348,570),15,'#2D5366');txt(d,(128,351),'PREVIEW',20,WHITE,True);txt(d,(42,578),'IMG_4821.jpg',14,WHITE,True);button(d,700,'Delete file',ORANGE);pages['32_media_preview']=im

for name,im in pages.items():im.save(OUT/(name+'.png'))
thumbw,thumbh=195,422;cols=5;rows=math.ceil(len(pages)/cols)
sheet=Image.new('RGB',(cols*thumbw+48,rows*(thumbh+46)+66),'#0A1020');sd=ImageDraw.Draw(sheet);txt(sd,(26,18),'CLEANSUPER AI  /  VISUAL SYSTEM V2',23,WHITE,True)
for i,(name,im) in enumerate(pages.items()):
    x=24+(i%cols)*thumbw;y=66+(i//cols)*(thumbh+46)
    sheet.paste(im.resize((180,390)),(x,y));txt(sd,(x,y+396),name[3:].replace('_',' ').title(),11,MUTED)
sheet.save(OUT/'overview.png')
print(f'Generated {len(pages)} screens in {OUT}')
