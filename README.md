# OutWatch - Functional and reactive UIs with Rx, VirtualDom and Scala [![Build Status](https://travis-ci.org/OutWatch/outwatch.svg?branch=master)](https://travis-ci.org/OutWatch/outwatch) [![Scala.js](https://www.scala-js.org/assets/badges/scalajs-0.6.6.svg)](https://www.scala-js.org) [![Gitter chat](https://badges.gitter.im/gitterHQ/gitter.png)](https://gitter.im/OutWatch/Lobby)


## Getting started

First you will need to install Java and SBT if you haven't already. 
Create a new SBT project and add the ScalaJS plugin to your `plugins.sbt`.
Then add the following line to your `build.sbt`.

    libraryDependencies += "io.github.outwatch" %%% "outwatch" % "0.5.2"
    
And you're done, you can now start building your own OutWatch app!
Please check out the [documentation](https://outwatch.github.io/) on how to proceed.


## Three main goals of OutWatch

1. Updating DOM efficiently without sacrificing abstraction => Virtual DOM
2. Handling subscriptions automatically
3. Removing or restricting the need for Higher Order Observables



## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/OutWatch/outwatch/issues).

## LICENSE

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

<https://www.apache.org/licenses/LICENSE-2.0>

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
