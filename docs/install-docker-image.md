---
layout: default
title: Install As Docker Image
---

Data Fence can be installed from [Docker Hub](https://github.com/zoom/zoom-data-fence).

```shell
docker pull zoomvideo/zoom-data-fence
```

# Deployment
Unlike most docker images, Data Fence is intended to be run as a batch job. Therefore, 
the best way to deploy the image is to mount the files you intend to use, provide any 
secrets referenced in your configuration and run whatever command you intend to run 
from within the container. 

This works well with most CI/CD tools. However, cloud orchestration systems can also 
work this way.
