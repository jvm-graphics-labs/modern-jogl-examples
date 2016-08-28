modern-jogl-examples
====================

Examples ported in JOGL from the tutorials "Learning Modern 3D Graphic Programming" by J.L.McKesson, (original bitbucket [repository](https://bitbucket.org/alfonse/gltut/overview)).

The original website (http://www.arcsynthesis.org/gltut) is down because probably the domain [expired](https://bitbucket.org/alfonse/gltut/issues/127/arcsynthesisorg-web-site). Pavel Rojtberg is continuing the manteinance of the tutorial [here](https://github.com/paroj/gltut). 

He is also supporting the html form of the documentation [here](https://paroj.github.io/gltut/), I strongly suggest you to read and refer it during the learning of each tutorial sample.

To run the examples, just follow these simple steps:

- clone the project

- download the dependencies from http://jogamp.org/deployment/jogamp-current/archive/jogamp-all-platforms.7z

- create a Library called "JOGL"

- import it and point it to the jogamp-all-platforms\jar\gluegen-rt.jar and jogamp-all-platforms\jar\jogl.jar

Remember to clean & build after cloning it!


In this project there are the original Jglm and the java unofficial opengl SDK. At the end of 2012 I decided to make them as separated projects, in order to make things clear and hope that they could grow their own. But you don't need them, the original ones work out of the box.



Ps: in the last jogl builds GL3 has been moved, so in the meanwhile I upgrade all my projects, you have to change it by yourself  :grimacing:

Pps: I am also rewriting the samples using the jogl [newt](http://jogamp.org/jogl/doc/NEWT-Overview.html).. be patient  :pray: 
