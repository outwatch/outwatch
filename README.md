# OutWatch - Functional and reactive UIs with Rx, VirtualDom and Scala


## Getting started

First you will need to install Java and SBT if you haven't already. 
Create a new SBT project and add the ScalaJS plugin to your `plugins.sbt`.
Then add the following line to your `build.sbt`.

    libraryDependencies += "org.outwatch" %%% "outwatch" % "0.3.2"
    
Great, we've created our first OutWatch Project!
Now we'll create a small Hello World app to get you started.
First we'll create a new Scala file `HelloOutWatch.scala` in our main directory.
Inside we'll want to import the framework by specifying `import outwatch.dom._` at the top of our file.
Now we're ready to create our main entry point:

    object HelloOutWatch extends JSApp {
      def main(): Unit = {
      
      }
    }



Then create an `index.html` file at your project's root.
Inside we'll want to 



## The Scan operator

`scan` is the most essential operator for FRP.
If you're from the Scala world, you're probably already familiar with `scan` from the collection API.
Users of Redux or other state containers can also feel similarly at home.
`scan` is a lot like `fold` or `reduce` but creates intermediary values every time.




## TypeScript or Scala.js

- Tooling inferior (alt+enter to import)
- Better Support for FP (Currying, EOP, ADTs, Lazy Evaluation, Immutability)
- Powerful Options for dealing with nulls
- Better Type system (https://gist.github.com/t0yv0/4449351 https://gist.github.com/valtron/5688638)

## Bugs and Feedback

For bugs, questions and discussions please use the [Github Issues](https://github.com/issues).

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
