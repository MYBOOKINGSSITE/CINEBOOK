const movies = [
  { title: "Maharaj", poster: "images/movie1.jpg", rating: "U/A", duration: "2h 16m", link: "#" },
  { title: "Kill", poster: "images/movie2.jpg", rating: "A", duration: "1h 55m", link: "#" },
  { title: "Deadpool & Wolverine", poster: "images/movie3.jpg", rating: "U/A", duration: "2h 8m", link: "#" },
  { title: "Sarfira", poster: "images/movie4.jpg", rating: "U", duration: "2h 20m", link: "#" },
  { title: "Stree 2", poster: "images/movie5.jpg", rating: "U/A", duration: "2h 10m", link: "#" },
  { title: "Bhool Bhulaiyaa 3", poster: "images/movie6.jpg", rating: "U/A", duration: "2h 6m", link: "#" }
];

const grid = document.getElementById("moviesGrid");
const search = document.getElementById("movieSearch");
const clearBtn = document.getElementById("clearSearch");
const countPill = document.getElementById("countPill");

// Render cards
function render(list){
  grid.innerHTML = "";
  list.forEach((m, idx) => {
    const card = document.createElement("article");
    card.className = "card";
    card.innerHTML = `
      <img class="poster" src="${m.poster}" alt="${m.title}">
      <div class="card-body">
        <h3 class="title">${m.title}</h3>
        <div class="meta">${m.rating} • ${m.duration}</div>
      </div>
      <div class="actions">
        <a class="btn" href="${m.link}" target="_blank" rel="noopener">Book Now</a>
      </div>
    `;
    grid.appendChild(card);
  });
  countPill.textContent = list.length + (list.length === 1 ? " Movie" : " Movies");
}

function updateHero(fromList){
  if(!fromList.length) return;
  const top = fromList[0];
  document.getElementById("heroTitle").textContent = top.title;
  document.getElementById("heroTag").textContent = top.duration + " • " + top.rating;
  const heroBtn = document.getElementById("heroBtn");
  heroBtn.href = top.link;
}

// Search
search.addEventListener("input", () => {
  const q = search.value.trim().toLowerCase();
  const filtered = movies.filter(m => m.title.toLowerCase().includes(q));
  render(filtered);
  updateHero(filtered.length ? filtered : movies);
});

clearBtn.addEventListener("click", () => {
  search.value = "";
  render(movies);
  updateHero(movies);
});

// Init
render(movies);
updateHero(movies);
document.getElementById("year").textContent = new Date().getFullYear();
