modern-jogl-examples
====================

This porting is free but needs your support to sustain its development. There are lots of desirable new features and maintenance to do. If you are an individual using dear imgui, please consider donating via Patreon or PayPal. If your company is using dear imgui, please consider financial support (e.g. sponsoring a few weeks/months of development)

Monthly donations via Patreon:
<br>[![Patreon](https://cloud.githubusercontent.com/assets/8225057/5990484/70413560-a9ab-11e4-8942-1a63607c0b00.png)](https://www.patreon.com/user?u=7285753)

One-off donations via PayPal:
<br>[![PayPal](https://www.paypalobjects.com/en_US/i/btn/btn_donate_LG.gif)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=KBKVJBQ3NNH8E)

<img src="./src/main/resources/screenshots/jogl.png" height="250px"> 

Examples ported in JOGL from the tutorials "Learning Modern 3D Graphic Programming" by J.L.McKesson, (original bitbucket [repository](https://bitbucket.org/alfonse/gltut/overview)).

The original website (http://www.arcsynthesis.org/gltut) is down because probably the domain [expired](https://bitbucket.org/alfonse/gltut/issues/127/arcsynthesisorg-web-site). Pavel Rojtberg is continuing the manteinance of the tutorial [here](https://github.com/paroj/gltut). 

He is also supporting the html form of the documentation [here](https://paroj.github.io/gltut/), I strongly suggest you to read and refer it during the learning of each tutorial sample.

You can find the examples in java under [`src/main/java`](https://github.com/java-opengl-labs/modern-jogl-examples/tree/master/src/main/java/main) and the corresponding in kotlin under [`src/main/kotlin`](https://github.com/java-opengl-labs/modern-jogl-examples/tree/master/src/main/kotlin/main)

Few comments on Kotlin: 

- it's awesome
- [`src/main/kotlin/main`](https://github.com/java-opengl-labs/modern-jogl-examples/tree/master/src/main/kotlin/main) is an example, showing also how you can exploit some overloading gl functions to reduce the boiler plate arguments
- [`src/main/kotlin/glNext`](https://github.com/java-opengl-labs/modern-jogl-examples/tree/master/src/main/kotlin/glNext) pushes reduction and expressiveness to the top, substituting many gl commands with constructs that rearrange some common gl patterns to bring *a lot* of code down

Status:

- [x] Chapter 1, Hello Triangle
- [x] Chapter 2, Playing with Colors
- [x] Chapter 3, Moving Triangle
- [x] Chapter 4, Objects at Rest
- [x] Chapter 5, Objects at Depth
- [x] Chapter 6, Objects in Motion
- [x] Chapter 7, World in Motion
- [x] Chapter 8, Getting Oriented
- [x] Chapter 9, Lights On
- [x] Chapter 10, Plane Lights
- [x] Chapter 11, Shinies
- [ ] Chapter 12, Dynamic Lights
- [x] Chapter 13, Lies and Impostors
- [x] Chapter 14, Textures are not Pictures
- [x] Chapter 15, Many Images
- [ ] Chapter 16, Gamma and Textures:
    - [x] Gamma Ramp
    - [x] Gamma Checkers
    - [ ] Gamma Landscape
- [ ] Chapter 17, Spotlight on Textures
