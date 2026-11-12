from PIL import Image, ImageDraw, ImageFont, ImageFilter
from pathlib import Path
import math, random

O=Path(__file__).parent; O.mkdir(exist_ok=True)
W,H=390,844
BG='#F5F8F5'; WHITE='#FFFFFF'; INK='#163128'; SUB='#72877D'; BLUE='#20B875'; DEEP='#11935B'; PALE='#E8F8EF'; RED='#F06B59'; LINE='#E5EEE8'
CH='/System/Library/Fonts/PingFang.ttc'
EN='/System/Library/Fonts/Avenir Next.ttc'
def f(n,b=False,cn=False): return ImageFont.truetype(CH if cn else EN,n,index=(0 if b else 7) if not cn else 0)
def text(d,x,y,s,n=16,c=INK,b=False):d.text((x,y),s,font=f(n,b,any(ord(a)>127 for a in s)),fill=c)
def rr(d,box,r=18,fill=WHITE,outline=None,w=1):d.rounded_rectangle(box,radius=r,fill=fill,outline=outline,width=w)
def center(d,y,s,n=16,c=INK,b=False):d.text(((W-d.textlength(s,font=f(n,b,any(ord(a)>127 for a in s))))/2,y),s,font=f(n,b,any(ord(a)>127 for a in s)),fill=c)
def line(d,xy,c=LINE,w=1):d.line(xy,fill=c,width=w)
def ico(d,x,y,kind='file',c=DEEP,size=38):
    rr(d,(x,y,x+size,y+size),12,PALE)
    if kind in ('photo','video','image'):
        rr(d,(x+10,y+10,x+size-10,y+size-10),3,None,c,2)
        d.ellipse((x+15,y+14,x+20,y+19),fill=c)
        line(d,(x+12,y+size-13,x+19,y+21,x+size-11,y+size-13),c,2)
    elif kind=='clean':
        d.arc((x+10,y+9,x+size-10,y+size-10),-50,250,fill=c,width=3)
        line(d,(x+18,y+23,x+size-12,y+23),c,3)
    else:
        rr(d,(x+12,y+9,x+size-12,y+size-9),3,None,c,2)
        line(d,(x+16,y+18,x+size-16,y+18),c,2)
        line(d,(x+16,y+24,x+size-16,y+24),c,2)
def btn(d,y,label='开始清理',color=BLUE):
    rr(d,(22,y,368,y+52),18,color);center(d,y+13,label,17,WHITE,True)
def header(title,tab=False,kicker=None):
    im=Image.new('RGB',(W,H),BG);d=ImageDraw.Draw(im)
    text(d,20,12,'11:08',13,'#58646B',True);text(d,318,12,'●  100%',12,'#58646B')
    if not tab:text(d,23,65,'‹',31,INK);text(d,58,68,title,23,INK,True)
    else:
        text(d,22,67,title,25,INK,True);rr(d,(335,60,371,96),18,WHITE)
        d.ellipse((344,69,362,87),outline=INK,width=3);d.ellipse((350,75,356,81),fill=INK)
    if kicker:text(d,22,111,kicker,13,SUB)
    if tab:
        rr(d,(0,771,390,844),0,WHITE);line(d,(0,771,390,771))
        tabs=[('首页','home'),('滑动清理','swipe'),('压缩','compress'),('工具','tools')]
        for i,(name,key) in enumerate(tabs):
            x=17+i*95;c=DEEP if title==name else '#95A2AB'
            if i==0:d.line((x+28,795,x+38,786,x+48,795),fill=c,width=3);rr(d,(x+31,796,x+45,808),3,None,c,2)
            elif i==1:d.arc((x+27,786,x+51,810),30,300,fill=c,width=3)
            elif i==2:d.polygon([(x+27,798),(x+38,786),(x+50,798),(x+38,810)],outline=c)
            else:
                for a in range(2):
                    for b in range(2):rr(d,(x+29+a*12,788+b*12,x+37+a*12,796+b*12),2,None,c,2)
            text(d,x+14,814,name,11,c,title==name)
    return im,d
def thumb(d,x,y,w=75,h=75,seed=1,blur=False):
    random.seed(seed)
    colors=[('#90B9C6','#315770'),('#E9B890','#866656'),('#A4CBA2','#4C7669'),('#B5A4C8','#574C7B'),('#D7BFA4','#668CB1')]
    a,b=colors[seed%len(colors)]
    rr(d,(x,y,x+w,y+h),11,a)
    d.ellipse((x+w*.55,y+h*.1,x+w*.85,y+h*.4),fill='#F6D3A6')
    d.polygon([(x,y+h*.8),(x+w*.43,y+h*.36),(x+w,y+h*.85),(x+w,y+h)],fill=b)
    if blur:rr(d,(x,y,x+w,y+h),11,'#7A99A1')
def section(d,y,title,more='查看全部'):
    text(d,22,y,title,18,INK,True);text(d,321,y+2,more,12,DEEP)
def item(d,y,title,sub='',kind='file',value=''):
    rr(d,(22,y,368,y+70),17,WHITE);ico(d,35,y+16,kind);text(d,85,y+12,title,16,INK,True)
    if sub:text(d,85,y+39,sub,12,SUB)
    if value:text(d,280,y+25,value,12,DEEP,True)
    text(d,343,y+22,'›',23,INK)
def grid(d,y,items):
    for i,(name,sub,kind) in enumerate(items):
        x=22+(i%2)*178; yy=y+(i//2)*111
        rr(d,(x,yy,x+168,yy+99),20,WHITE)
        ico(d,x+14,yy+13,kind,size=37)
        text(d,x+14,yy+59,name,14,INK,True)
        text(d,x+105,yy+61,sub,11,SUB)
def gallery(d,y,title,seed=0):
    rr(d,(22,y,368,y+155),21,WHITE);ico(d,36,y+16,'photo',size=30)
    text(d,77,y+17,title,17,INK,True);text(d,344,y+12,'›',24,INK)
    for k in range(4):thumb(d,36+k*80,y+56,72,75,seed+k)
def storage(d,y=375):
    rr(d,(22,y,368,y+185),23,WHITE);ico(d,39,y+20,'file',size=30)
    text(d,80,y+19,'存储空间总览',18,INK,True);text(d,340,y+16,'›',24,INK)
    d.arc((45,y+63,169,y+179),-90,215,fill='#E5EAED',width=12)
    d.arc((45,y+63,169,y+179),-90,-30,fill=RED,width=12)
    text(d,76,y+101,'16%',26,INK,True);text(d,88,y+134,'已用',12,SUB)
    d.ellipse((204,y+73,213,y+82),fill=BLUE);text(d,222,y+62,'已用',12,SUB);text(d,285,y+59,'17.48 GB',14,INK,True)
    d.ellipse((204,y+103,213,y+112),fill='#85959B');text(d,222,y+92,'可用',12,SUB);text(d,285,y+89,'90.79 GB',14,INK,True)
    line(d,(200,y+127,348,y+127));text(d,201,y+136,'可清理 716 MB',17,RED,True)
def listscreen(title,sub,items,kind='file',metric='716 MB'):
    im,d=header(title,kicker=sub)
    rr(d,(22,148,368,254),22,WHITE);text(d,40,164,'可清理空间',14,SUB);text(d,40,190,metric,31,RED,True)
    rr(d,(276,173,350,227),16,PALE);ico(d,293,181,kind,size=38)
    section(d,281,'建议检查','全选')
    for i,(name,detail) in enumerate(items):item(d,323+i*78,name,detail,kind)
    btn(d,755,'查看并清理')
    return im
pages={}
im,d=header('首页',True)
rr(d,(22,120,368,170),23,PALE,BLUE);ico(d,35,129,'file',size=30);text(d,78,130,'添加桌面小工具',15,INK,True);text(d,78,151,'照片、视频与清理快捷入口',11,SUB)
rr(d,(22,187,368,357),24,BLUE);text(d,43,213,'快速扫描',25,WHITE,True);text(d,43,253,'查找重复文件与可清理空间',13,WHITE)
rr(d,(43,290,186,335),14,WHITE);text(d,69,300,'检查空间',17,DEEP,True)
d.ellipse((235,210,347,322),outline='#B3ECFB',width=11);d.ellipse((248,223,334,309),outline=WHITE,width=4);ico(d,273,247,'clean',size=36)
storage(d,374);section(d,584,'智能建议')
gallery(d,618,'相似照片',1)
pages['01_home']=im
im,d=header('滑动清理',True,'向左保留，向右删除，轻松整理照片')
rr(d,(22,149,368,626),24,WHITE);thumb(d,37,165,316,376,2)
rr(d,(38,547,352,610),16,PALE);text(d,55,557,'2026 年 8 月',17,INK,True);text(d,55,583,'128 张照片待整理',12,SUB)
btn(d,660,'开始整理照片');pages['02_swipe']=im
im,d=header('压缩',True,'缩小文件，保留重要内容')
rr(d,(22,150,368,334),23,WHITE);ico(d,42,173,'video',size=52);text(d,110,175,'视频压缩',21,INK,True);text(d,110,209,'大视频占用空间？',13,SUB)
rr(d,(42,264,348,313),14,BLUE);center(d,276,'选择视频',17,WHITE,True)
rr(d,(22,351,368,535),23,WHITE);ico(d,42,374,'photo',size=52);text(d,110,376,'图片压缩',21,INK,True);text(d,110,410,'清晰画面，更小体积',13,SUB)
rr(d,(42,465,348,514),14,PALE);center(d,477,'选择图片',17,DEEP,True)
section(d,565,'压缩前后');item(d,607,'画质由你决定','保存前可以预览结果','video')
pages['03_compress']=im
im,d=header('工具',True,'常用工具与设备信息')
grid(d,148,[('大文件清理','空间','file'),('音频清理','音频','file'),('应用管理','应用','file'),('联系人','通讯录','file')])
section(d,388,'更多工具','')
for i,(a,b,k) in enumerate([('隐私保险箱','保护个人文件','file'),('图片增强','改善照片画质','photo'),('网络测速','测试网络速度','file'),('电池信息','查看电池状态','file')]):item(d,429+i*78,a,b,k)
pages['04_tools']=im
im,d=header('智能清理',kicker='扫描完成，以下内容可供检查')
rr(d,(22,151,368,303),24,BLUE);text(d,43,171,'预计可清理',15,WHITE);text(d,43,205,'4.6 GB',39,WHITE,True);text(d,43,258,'共 312 个项目',13,WHITE)
section(d,332,'清理建议','全选')
grid(d,373,[('相似照片','1.8GB','photo'),('重复视频','1.4GB','video'),('缓存文件','1.2GB','file'),('联系人','12组','file')])
btn(d,755,'查看清理结果');pages['05_smart_clean']=im
data=[
('相似照片','从相似照片中保留最喜欢的', [('海边旅行','12 张相似照片'),('家庭时光','8 张相似照片'),('城市漫步','6 张相似照片')],'photo'),
('重复照片','检查完全相同的照片',[('相机照片','4 个副本'),('旅行相册','3 个副本'),('下载图片','2 个副本')],'photo'),
('截图清理','整理不再需要的截图',[('今天','14 张截图'),('本周','28 张截图'),('更早','40 张截图')],'photo'),
('重复视频','移除重复的视频文件',[('旅行视频','3 个副本'),('生日片段','2 个副本'),('屏幕录制','2 个副本')],'video'),
('全部视频','查看设备中的视频',[('相机视频','1.2 GB'),('屏幕录制','860 MB'),('下载视频','520 MB')],'video'),
('大文件清理','先看占用空间最大的文件',[('4K 旅行视频','1.6 GB'),('离线电影','980 MB'),('备份文件','740 MB')],'file'),
('缓存清理','清理可安全移除的临时文件',[('相册缓存','420 MB'),('社交应用','380 MB'),('浏览器缓存','210 MB')],'file'),
('联系人清理','合并重复的联系人',[('Alex Morgan','3 条联系人'),('Jamie Lee','2 条联系人'),('Sam Rivera','2 条联系人')],'file'),
('应用管理','查看应用占用空间',[('视频编辑','1.3 GB'),('社交应用','820 MB'),('离线地图','640 MB')],'file'),
('图片清理','查看设备中的图片',[('相机','1420 张图片'),('下载','186 张图片'),('编辑结果','94 张图片')],'photo'),
('视频清理','查看设备中的视频',[('相机','34 个视频'),('下载','12 个视频'),('屏幕录制','8 个视频')],'video'),
('音频清理','查看设备中的音频',[('音乐','228 个文件'),('录音','42 个文件'),('下载','19 个文件')],'file')]
for i,(title,sub,items,kind) in enumerate(data,6):pages[f'{i:02d}_{title}']=listscreen(title,sub,items,kind)
special=[
('照片详情','滑动查看待整理的照片','查看照片'),('视频压缩','选择画质与压缩比例','开始压缩'),
('隐私保险箱','使用设备锁保护私人文件','解锁保险箱'),('图片增强','让照片更清晰、更明亮','选择照片'),
('网络测速','了解当前网络速度','开始测速'),('网络流量','查看应用使用的网络流量','查看详情'),
('电池信息','了解电池健康与充电状态','查看详情'),('设备信息','查看存储、内存与系统版本','查看详情'),
('日历清理','整理过期和重复日程','查看日程'),('清理完成','空间已释放，设备更清爽','返回首页'),
('设置','管理语言、通知与隐私','查看设置'),('语言','选择使用语言','保存'),
('隐私政策','了解数据如何处理','阅读完整政策'),('使用条款','查看产品使用说明','阅读完整条款'),
('媒体预览','检查文件后再决定清理','删除文件')]
for i,(title,sub,action) in enumerate(special,18):
    im,d=header(title,kicker=sub)
    if title in ('照片详情','媒体预览','图片增强'):
        rr(d,(22,147,368,603),23,WHITE);thumb(d,36,162,318,426,i)
    elif title=='清理完成':
        rr(d,(22,160,368,505),23,WHITE);d.ellipse((132,199,258,325),fill=PALE);center(d,232,'✓',52,DEEP,True);center(d,347,'2.4 GB 已清理',27,INK,True);center(d,392,'128 个项目已安全处理',14,SUB)
    elif title=='设置':
        for k,(a,b) in enumerate([('语言','选择使用语言'),('通知','管理提醒'),('隐私政策','了解数据使用'),('使用条款','产品说明')]):item(d,151+k*80,a,b)
    elif title=='语言':
        for k,a in enumerate(['简体中文  ✓','English','Español','Français','Português']):item(d,151+k*78,a)
    elif title in ('隐私政策','使用条款'):
        rr(d,(22,151,368,703),22,WHITE);text(d,43,177,title,22,INK,True);text(d,43,222,'清晰说明，方便阅读。',14,SUB)
    else:
        rr(d,(22,151,368,392),23,WHITE);ico(d,154,192,'video' if '视频' in title else 'file',size=82)
        center(d,307,title,23,INK,True)
        section(d,428,'功能详情','');item(d,470,'查看与管理',sub)
    if title not in ('设置','隐私政策','使用条款'):btn(d,755,action)
    pages[f'{i:02d}_{title}']=im
for name,im in pages.items():im.save(O/(name+'.png'))
cols=5;tw,th=195,422;rows=math.ceil(len(pages)/cols)
sheet=Image.new('RGB',(cols*tw+48,rows*(th+44)+84),'#EAF0F4');sd=ImageDraw.Draw(sheet)
text(sd,25,22,'Storage Cleaner  /  UI concept V3',24,INK,True)
for i,(name,im) in enumerate(pages.items()):
    x=24+(i%cols)*tw;y=74+(i//cols)*(th+44);sheet.paste(im.resize((180,390)),(x,y))
    text(sd,x,y+395,name.split('_',1)[1],11,INK,True)
sheet.save(O/'overview.png')
print(len(pages))
