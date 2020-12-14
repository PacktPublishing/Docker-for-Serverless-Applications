## $5 Tech Unlocked 2021!
[Buy and download this Book for only $5 on PacktPub.com](https://www.packtpub.com/product/docker-for-serverless-applications/9781788835268)
-----
*If you have read this book, please leave a review on [Amazon.com](https://www.amazon.com/gp/product/1788835263).     Potential readers can then use your unbiased opinion to help them make purchase decisions. Thank you. The $5 campaign         runs from __December 15th 2020__ to __January 13th 2021.__*

# Docker for Serverless Applications
This is the code repository for [Docker for Serverless Applications](https://www.packtpub.com/virtualization-and-cloud/docker-serverless-applications?utm_source=github&utm_medium=repository&utm_campaign=9781788835268), published by [Packt](https://www.packtpub.com/?utm_source=github). It contains all the supporting project files necessary to work through the book from start to finish.
## About the Book
Serverless applications have gained a lot of popularity among developers and are currently the buzzwords in the tech market. Docker and serverless are two terms that go hand-in-hand.

This book will start by explaining serverless and Function-as-a-Service (FaaS) concepts, and why they are important. Then, it will introduce the concepts of containerization and how Docker fits into the Serverless ideology. It will explore the architectures and components of three major Docker-based FaaS platforms, how to deploy and how to use their CLI. Then, this book will discuss how to set up and operate a production-grade Docker cluster. We will cover all concepts of FaaS frameworks with practical use cases, followed by deploying and orchestrating these serverless systems using Docker. Finally, we will also explore advanced topics and prototypes for FaaS architectures in the last chapter.

By the end of this book, you will be in a position to build and deploy your own FaaS platform using Docker.

## Instructions and Navigation
All of the code is organized into folders. For example, Chapter03.


The code will look like the following:
```

def find_layers(img, id):
    with closing(img.extractfile('%s/json' % id)) as fd:
        info = json.load(fd)

    LOG.debug('layer = %s', id)
    for k in ['os', 'architecture', 'author', 'created']:
        if k in info:
            LOG.debug('%s = %s', k, info[k])
```

## Related Products
* [Hands-On Docker for Microservices [Video]](https://www.packtpub.com/application-development/hands-docker-microservices-video?utm_source=github&utm_medium=repository&utm_campaign=9781788999960)

* [Working with Advanced Docker Operations [Video]](https://www.packtpub.com/virtualization-and-cloud/working-advanced-docker-operations-video?utm_source=github&utm_medium=repository&utm_campaign=9781788471695)

* [Deployment with Docker](https://www.packtpub.com/virtualization-and-cloud/deployment-docker?utm_source=github&utm_medium=repository&utm_campaign=9781786469007)

