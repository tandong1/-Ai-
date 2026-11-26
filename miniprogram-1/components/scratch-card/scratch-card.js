Component({
  lifetimes: {
    ready() {
      this.createSelectorQuery().select('#scratchCanvas').fields({ node: true, size: true }).exec(res => {
        if (!res[0] || !res[0].node) return;
        const { node, width, height } = res[0];
        const dpr = wx.getWindowInfo().pixelRatio;
        node.width = width * dpr;
        node.height = height * dpr;
        const ctx = node.getContext('2d');
        ctx.scale(dpr, dpr);
        const gradient = ctx.createLinearGradient(0, 0, width, height);
        gradient.addColorStop(0, '#b7c7df');
        gradient.addColorStop(1, '#e0e8f5');
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, width, height);
        ctx.fillStyle = '#35476b';
        ctx.textAlign = 'center';
        ctx.font = 'bold 19px sans-serif';
        ctx.fillText('刮开奖励', width / 2, height / 2 - 6);
        ctx.font = '12px sans-serif';
        ctx.fillText('用手指涂一涂', width / 2, height / 2 + 22);
        this.ctx = ctx;
        this.surface = { width, height };
        this.cells = new Set();
      });
    }
  },
  methods: {
    start(e) { this.last = null; this.erase(e); },
    move(e) { this.erase(e); },
    end() { this.last = null; },
    erase(e) {
      if (!this.ctx || this.done || !e.touches.length) return;
      const { x, y } = e.touches[0];
      const ctx = this.ctx;
      const from = this.last || { x, y };
      const radius = 22;
      ctx.globalCompositeOperation = 'destination-out';
      ctx.lineWidth = radius * 2;
      ctx.lineCap = 'round';
      ctx.beginPath();
      ctx.moveTo(from.x, from.y);
      ctx.lineTo(x, y);
      ctx.stroke();
      ctx.beginPath();
      ctx.arc(x, y, radius, 0, Math.PI * 2);
      ctx.fill();
      // 用网格统计实际擦除范围，重复涂同一位置不会增加进度。
      const { width, height } = this.surface;
      const steps = Math.max(1, Math.ceil(Math.hypot(x - from.x, y - from.y) / 8));
      for (let step = 0; step <= steps; step++) {
        const px = from.x + (x - from.x) * step / steps;
        const py = from.y + (y - from.y) * step / steps;
        for (let row = 0; row < 12; row++) {
          for (let col = 0; col < 24; col++) {
            if (Math.hypot((col + 0.5) * width / 24 - px, (row + 0.5) * height / 12 - py) <= radius)
              this.cells.add(row * 24 + col);
          }
        }
      }
      this.last = { x, y };
      if (this.cells.size / 288 >= 0.4) {
        this.done = true;
        this.triggerEvent('reveal');
      }
    }
  }
});
