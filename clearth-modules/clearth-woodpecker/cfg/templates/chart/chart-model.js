class ChartModel {
    constructor(datasets, labels) {
        this.datasets = datasets;
        this.labels = labels;
        this.divider = 1;
        this.max = 0;
    }
    generateSeries() {
        let generatedSeries = [];
            Object.keys(this.datasets).forEach(datasetName => {
                let dividedSeries = [];
                for(let i = 0; i < this.datasets[datasetName].points.length; i++)
                {
                    if(((i) % this.divider) == 0)
                    {
                        dividedSeries.push(this.datasets[datasetName].points[i]);
                    }
                }
                generatedSeries.push(dividedSeries);
            });
        return generatedSeries;
    }
    
    getMax() {
      Object.keys(this.datasets).forEach(datasetName => {
          let dataset = this.datasets[datasetName];
          dataset.points.forEach(point => {
              if (point.y > this.max) {
                this.max = point.y;
              }
          });
      });
      return this.max;
    }
}