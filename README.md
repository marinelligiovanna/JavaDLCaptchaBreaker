Java Deep Learning Captcha Breaker
===================

This project aims to adapt a Machine Learning trained in Python Keras to run in Java Tensorflow. The code was built considering the model built in my Python project [Deep Learning Captcha Breaker](https://github.com/marinelligiovanna/DLCaptchaBreaker).

The model was converted from Python .h5 file to .pb file using the class Keras2Tensorflow included in the Python project. Note that this class is suitable not only for Java Tensorflow, but also for C# or C++ Tensorflow, since it uses the same kind of file. 

An example of how you can call the DLDecaptcher class inside your code:

```java
DLDecaptcher decaptcher = new DLDecaptcher();

File imageFile = new File("\\img286.jpg");
byte[] imageBytes = Files.readAllBytes(imageFile.toPath());
String captcha = decaptcher.decapcha(imageBytes);
System.out.println(captcha);
```
Some captcha images were included in the resources file as example.

If you have any doubts, feel free to contact me.
