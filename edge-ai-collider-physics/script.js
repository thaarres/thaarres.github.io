
const slides=[...document.querySelectorAll(".slide")];
let index=0, locked=false;

function fillDots(selector,n,hotEvery=0){
  document.querySelectorAll(selector).forEach(el=>{
    if(el.children.length) return;
    for(let i=0;i<n;i++){
      const d=document.createElement("i"); d.className="dot";
      if(hotEvery && i===Math.floor(n*.73)) d.classList.add("hot");
      if(selector.includes("pixel") && (i%37===0 || i%53===0)) d.classList.add("cyan");
      el.appendChild(d);
    }
  });
}
fillDots(".rare-field",220,1);
fillDots(".collision-grid",256,1);
fillDots(".pixel-matrix",176,0);

function animateCount(slide){
  slide.querySelectorAll(".count").forEach(el=>{
    const target=+el.dataset.target, start=performance.now(), dur=1200;
    function tick(t){let p=Math.min(1,(t-start)/dur); p=1-Math.pow(1-p,3); el.textContent=Math.round(target*p).toLocaleString(); if(p<1)requestAnimationFrame(tick)}
    requestAnimationFrame(tick);
  });
}
function show(n){
  if(locked)return;
  n=Math.max(0,Math.min(slides.length-1,n));
  if(n===index && slides[n].classList.contains("active")) return;
  locked=true;
  slides[index]?.classList.remove("active");
  index=n; slides[index].classList.add("active");
  document.querySelector("#progress").style.width=((index+1)/slides.length*100)+"%";
  document.querySelector("#counter").textContent=String(index+1).padStart(2,"0")+" / "+String(slides.length).padStart(2,"0")+" · "+slides[index].dataset.section;
  history.replaceState(null,"","#"+(index+1));
  animateCount(slides[index]);
  setTimeout(()=>locked=false,420);
}
function next(){show(index+1)} function prev(){show(index-1)}
addEventListener("keydown",e=>{
  if(["ArrowRight"," ","PageDown","Enter"].includes(e.key)){e.preventDefault();next()}
  if(["ArrowLeft","PageUp","Backspace"].includes(e.key)){e.preventDefault();prev()}
  if(e.key==="Home")show(0); if(e.key==="End")show(slides.length-1);
  if(e.key.toLowerCase()==="f" && document.documentElement.requestFullscreen) document.documentElement.requestFullscreen();
});
addEventListener("click",e=>{ if(e.target.closest("a"))return; e.clientX<innerWidth*.28?prev():next() });
let h=parseInt(location.hash.slice(1)); index=Math.max(0,Math.min(slides.length-1,(h||1)-1));
slides.forEach(s=>s.classList.remove("active")); slides[index].classList.add("active");
document.querySelector("#progress").style.width=((index+1)/slides.length*100)+"%";
document.querySelector("#counter").textContent=String(index+1).padStart(2,"0")+" / "+String(slides.length).padStart(2,"0")+" · "+slides[index].dataset.section;
animateCount(slides[index]);
