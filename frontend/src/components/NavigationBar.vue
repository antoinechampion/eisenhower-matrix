<template>
  <nav>
    <div class="nav__brand">
      <strong>Eisenhower Matrix</strong>
      <span>Free priority matrix editor</span>
    </div>
    <div class="nav__links">
      <a href="/">Home</a>
      <a href="/faq">FAQ</a>
    </div>
  </nav>
</template>

<style scoped>
nav {
  width: 100%;
  overflow: hidden;
  display: flex;
  justify-content: space-around;
  background: white;
  border-bottom: 1px solid #e0e0e0;
  padding: 8px 0;
}
.nav__brand {
  display: flex;
  flex-direction: column;
  margin: auto 24px;
}
.nav__brand strong {
  font-size: 1rem;
  font-weight: 700;
  color: #1a1a1a;
}
.nav__brand span {
  font-size: 0.85rem;
  color: #666;
  font-weight: 300;
}
.nav__links {
  display: flex;
  align-items: center;
}
.nav__links a {
  margin: auto 24px;
  text-decoration: underline;
  text-decoration-style: dotted;
  text-underline-offset: 2px;
  color: #2a2a2a;
  font-size: 0.95rem;
}
</style>
