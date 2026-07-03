<script setup lang="ts">
import NavigationBar from '@/components/NavigationBar.vue'
</script>

<template>
  <div class="page">
    <NavigationBar />

    <div class="container">
      <details>
        <summary>What is the Eisenhower Matrix?</summary>
        <p>
          The Eisenhower Matrix, also known as the Urgent-Important Matrix, is a tool used to
          prioritize tasks based on their level of importance and urgency. It was developed by
          <a href="https://en.wikipedia.org/wiki/Dwight_D._Eisenhower">President Dwight D. Eisenhower</a>
          and is designed to help individuals and organizations make better decisions about how to
          allocate their time and resources. It was later popularized by Stephen Covey's book,
          <a href="https://www.amazon.com/Habits-Highly-Effective-People-Powerful/dp/0743269519">The 7 Habits of Highly Effective People</a>.
        </p>
        <p>
          The matrix consists of four quadrants: important and urgent, important but not urgent,
          urgent but not important, and neither urgent nor important. Tasks that are important and
          urgent should be completed first, followed by tasks that are important but not urgent.
          Tasks that are urgent but not important should be delegated or eliminated, and tasks that
          are neither urgent nor important should be avoided.
        </p>
        <p>
          Using the Eisenhower Matrix can help you focus on the tasks that are most important and
          valuable, and avoid wasting time and energy on tasks that are not as crucial.
        </p>
      </details>

      <details>
        <summary>How to use effectively the Eisenhower Matrix?</summary>
        <p>
          Tasks that are both urgent and important require immediate action. These tasks typically
          have visible deadlines and consequences for delay in taking action. These tasks can be
          either externally imposed or ones that you have procrastinated on, but they require a
          crisis mode response.
        </p>
        <p>
          Tasks that are not urgent but important contribute to the achievement of long-term goals.
          These tasks may not have a deadline or end date, which can make it tempting to prioritize
          more urgent tasks instead. However, these tasks have a significant impact on your
          long-term effectiveness in achieving your goals.
        </p>
        <p>
          Tasks that are urgent but not important can be described as busy work. These tasks are
          often based on expectations set by others and do not contribute to the achievement of
          your long-term goals.
        </p>
        <p>
          Tasks that are neither urgent nor important are time-wasting activities that should be
          eliminated. These tasks do not contribute to progress towards your goals and can consume
          large amounts of time.
        </p>
        <p>
          <a href="https://todoist.com/productivity-methods/eisenhower-matrix">Here is an article</a>
          that provides more detailed information on how to determine which tasks belong in which
          quadrants. Ultimately, it is up to you to decide how you wish to prioritize your tasks.
        </p>
      </details>

      <details>
        <summary>What is this website? Is it really free?</summary>
        <p>
          This website was created because its author did not find an Eisenhower matrix editor that
          suited his needs, hence he created his own and made it available for free and
          <a href="https://github.com/antoinechampion/eisenhower-matrix">open-source</a>.
        </p>
      </details>

      <details>
        <summary>How to use the editor?</summary>
        <p>
          The first step is to create an account on the <a href="/">home page</a>. You will then be
          redirected to the matrix editor. On the bottom of the page, you will see a 'Plus' button
          to create a new task. You can drag your task on the desired quadrant of the board based on
          its priority. You can delete your task using the 'Minus' button. To create a memo document
          and attach it to a task, select a task and click on the 'Document' button.
        </p>
      </details>

      <details>
        <summary>What are the alternatives to this website?</summary>
        <p>Other matrix editors can be found online:</p>
        <ul>
          <li>
            <a href="https://miro.com/templates/priority-matrix/">Miro</a>,
            <a href="https://www.lucidchart.com/pages/templates/eisenhower-matrix">Lucidchart</a>,
            <a href="https://www.figma.com/templates/eisenhower-matrix-template/">Figma</a>...:
            Those are not standalone apps but templates for diagram building applications. They offer
            the best alternatives, with a polished user experience. However, their free tier is often
            very limited, and there is no way to easily attach a memo document to a task.
          </li>
          <li>
            <a href="https://appfluence.com/">Appfluence</a>: Another matrix editor with a solid free
            tier. However, you can't drag your post-its freely on the board: you instead need to order
            them sequentially.
          </li>
          <li>
            <a href="https://www.eisenhower.me/">Eisenhower.me</a>: Paid app for mobile. You can't
            drag your tasks freely on the board, you can't attach documents.
          </li>
        </ul>
      </details>

      <details>
        <summary>Who created this website?</summary>
        <p>You can find more about the author <a href="https://antoinechampion.com">here</a>.</p>
      </details>

      <details>
        <summary>How to contribute?</summary>
        <p>
          If you are a programmer, you are welcome to fork
          <a href="https://github.com/antoinechampion/eisenhower-matrix">the project's repository</a>
          and open a pull request. Otherwise, letting the world know about this website means a lot
          as well.
        </p>
      </details>
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
  padding: 32px 40px 48px;
}
details {
  background: white;
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 12px 20px;
  margin-bottom: 12px;
}
summary {
  cursor: pointer;
  font-weight: 600;
  color: #1a1a1a;
}
details p {
  margin: 12px 0;
}
details a {
  color: #1c6ca1;
}
</style>
