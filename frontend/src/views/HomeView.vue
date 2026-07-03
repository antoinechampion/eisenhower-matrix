<script setup lang="ts">
import NavigationBar from '@/components/NavigationBar.vue'
import LoginForm from '@/components/LoginForm.vue'
</script>

<template>
  <div class="page">
    <NavigationBar />

    <div class="container">
      <p class="short-description">
        &ldquo; The
        <a href="https://en.wikipedia.org/wiki/Time_management#The_Eisenhower_Method">Eisenhower Matrix</a>
        (or Priority Matrix) is a tool used to prioritize tasks based on their level of importance and urgency.
        It consists of four quadrants and tasks are prioritized based on their placement in these quadrants. &rdquo;
      </p>

      <div class="demo-and-login">
        <LoginForm />
        <div class="demo-card">
          <video poster="/images/preview.jpg" autoplay muted loop>
            <source src="/images/demo.mp4" type="video/mp4" />
          </video>
        </div>
      </div>

      <div class="features-row">
        <div class="features-card">
          <ul>
            <li>📌 Create tasks and prioritize them on the board,</li>
            <li>📄 Attach documents to go in greater details about a task,</li>
            <li>⚙️ Standalone web app (not a third party plugin or template)</li>
            <li>📱 Mobile ready,</li>
            <li>👍🏻 <a href="https://github.com/antoinechampion/eisenhower-matrix">Free and open-source</a> project.</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  background: #f5f5f5;
}
.container {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 40px 48px;
}
.short-description {
  margin-top: 32px;
  margin-bottom: 24px;
  font-size: 1.05rem;
  font-weight: 300;
  line-height: 1.7;
}
.short-description a {
  color: #1c6ca1;
}
.demo-and-login {
  display: flex;
  justify-content: center;
  gap: 24px;
  align-items: start;
  margin-bottom: 32px;
}
.demo-card {
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  overflow: hidden;
  width: fit-content;
}
.demo-card video {
  display: block;
  height: 318px;
  width: auto;
}
.features-row {
  display: flex;
  justify-content: center;
}
.features-card {
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 24px 40px;
}
.features-card ul {
  list-style: none;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 0.95rem;
  line-height: 1.6;
}
.features-card a {
  color: #1c6ca1;
}
@media (max-width: 700px) {
  .demo-and-login {
    flex-direction: column;
    align-items: center;
  }
  .short-description {
    margin-top: 12px;
    margin-bottom: 12px;
    font-size: 0.8rem;
    font-weight: 300;
    line-height: 1;
  }
}
</style>
