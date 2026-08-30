(function(){
  let slides=[],index=0,timer=null;
  const $=id=>document.getElementById(id);
  function normalize(list){return (list||[]).map(x=>typeof x==='string'?{src:x}:x).map(x=>({src:x.largeSrc||x.src||x.url,title:x.title||'Eden Family School'})).filter(x=>x.src);}
  function render(){
    const track=$('edenGalleryTrack'),dots=$('edenGalleryDots'),count=$('edenGalleryCount');
    if(!track||!dots||!slides.length)return;
    track.innerHTML=slides.map((s,i)=>`<div class="gallery-slide ${i===index?'is-active':''}"><img src="${s.src}" alt="${s.title}" ${i?'loading="lazy"':''}></div>`).join('');
    dots.innerHTML=slides.map((_,i)=>`<button class="${i===index?'active':''}" data-index="${i}" aria-label="Photo ${i+1}"></button>`).join('');
    dots.querySelectorAll('button').forEach(b=>b.addEventListener('click',()=>go(+b.dataset.index)));
    count.textContent=String(index+1).padStart(2,'0');
  }
  function go(n){if(!slides.length)return;index=(n+slides.length)%slides.length;render();restart();}
  function restart(){clearInterval(timer);if(slides.length>1)timer=setInterval(()=>go(index+1),5500);}
  async function init(){
    /* Use the site's real, already-loaded gallery source first. */
    if(typeof window.GALLERY_IMAGES!=='undefined')slides=normalize(window.GALLERY_IMAGES);
    render();
    /* Then append photographs uploaded through the admin gallery. */
    try{
      if(typeof firebase!=='undefined'&&firebase.database){
        const snap=await firebase.database().ref('gallery-photos').once('value');
        const v=snap.val();
        const admin=normalize(v?Object.values(v):[]);
        const seen=new Set(slides.map(x=>x.src));
        slides=slides.concat(admin.filter(x=>!seen.has(x.src)));
        render();restart();
      }
    }catch(e){console.warn('Eden gallery Firebase:',e);}
    restart();
  }
  function bind(){
    $('edenGalleryPrev')?.addEventListener('click',()=>go(index-1));
    $('edenGalleryNext')?.addEventListener('click',()=>go(index+1));
    init();
  }
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',bind);else bind();
})();
