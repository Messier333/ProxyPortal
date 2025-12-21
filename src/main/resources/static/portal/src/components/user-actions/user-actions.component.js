class UserActions extends Component {
  template() {
    return `
      <div class="actions">
        <form method="post" action="/logout" class="logout-form">
          <button type="submit" class="btn">Logout</button>
        </form>

        <a class="btn" href="/dashboard">Dashboard</a>
      </div>
    `;
  }

  connectedCallback() {
  super.connectedCallback?.();
  this.addEventListener("click", (e) => e.stopPropagation(), true);
}

  style() {
    return `
      .actions{
        display:flex;
        align-items:center;
        gap:10px;
      }

      .logout-form{
        margin:0;
      }

      .btn{
        background: transparent;
        border: 0;
        color: inherit;
        cursor: pointer;
        padding: 6px 10px;
        border-radius: 8px;
        font: inherit;
        text-decoration: none;
        opacity: .9;
      }

      .btn:hover{
        opacity: 1;
        background: rgba(255,255,255,0.10);
      }
    `;
  }
}
