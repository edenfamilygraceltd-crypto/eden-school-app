(function(){
  let slides=[],index=0,timer=null,paused=false,touchStartX=0;
  const $=id=>document.getElementById(id);
  function normalize(list){
    return (list||[]).map(x=>typeof x==='string'?{src:x}:x)
      .map(x=>({src:x.largeSrc||x.src||x.url,title:x.title||'Eden Family School'}))
      .filter(x=>x.src);
  }
  function render(){
    const track=$('edenGalleryTrack'),dots=$('edenGalleryDots'),count=$('edenGalleryCount');
    if(!track||!dots||!slides.length)return;
    index=(index+slides.length)%slides.length;
    track.innerHTML=slides.map((s,i)=>`<div class="gallery-slide ${i===index?'is-active':''}">
      <img src="${s.src}" alt="${s.title}" ${i?'loading="lazy"':''} decoding="async">
    </div>`).join('');
    dots.innerHTML=slides.map((_,i)=>`<button type="button" class="${i===index?'active':''}" data-index="${i}" aria-label="Photo ${i+1} sur ${slides.length}"></button>`).join('');
    dots.querySelectorAll('button').forEach(b=>b.addEventListener('click',()=>go(Number(b.dataset.index))));
    if(count)count.textContent=String(index+1).padStart(2,'0');
  }
  function go(n){
    if(!slides.length)return;
    index=(n+slides.length)%slides.length;
    render();
    restart();
  }
  function restart(){
    clearInterval(timer);timer=null;
    if(!paused&&slides.length>1)timer=setInterval(()=>go(index+1),5500);
  }
  async function init(){
    if(Array.isArray(window.GALLERY_IMAGES))slides=normalize(window.GALLERY_IMAGES);
    render();restart();
    try{
      if(window.firebase&&firebase.database){
        const snap=await firebase.database().ref('gallery-photos').once('value');
        const v=snap.val();
        const admin=normalize(v?Object.values(v):[]);
        const seen=new Set(slides.map(x=>x.src));
        slides=slides.concat(admin.filter(x=>!seen.has(x.src)));
        render();restart();
      }
    }catch(e){console.warn('Eden gallery Firebase:',e);}
  }
  function bind(){
    const stage=$('edenGalleryStage');
    $('edenGalleryPrev')?.addEventListener('click',()=>go(index-1));
    $('edenGalleryNext')?.addEventListener('click',()=>go(index+1));
    stage?.addEventListener('mouseenter',()=>{paused=true;restart();});
    stage?.addEventListener('mouseleave',()=>{paused=false;restart();});
    stage?.addEventListener('touchstart',e=>{touchStartX=e.changedTouches[0].clientX;},{passive:true});
    stage?.addEventListener('touchend',e=>{
      const dx=e.changedTouches[0].clientX-touchStartX;
      if(Math.abs(dx)>45)go(dx<0?index+1:index-1);
    },{passive:true});
    document.addEventListener('keydown',e=>{
      if(!stage||!stage.matches(':hover'))return;
      if(e.key==='ArrowLeft')go(index-1);
      if(e.key==='ArrowRight')go(index+1);
    });
    init();
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',bind);else bind();
})();
