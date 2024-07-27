# Web Bulb App for Android

Welcome to the repo for my simple weather app!

<img src="https://github.com/user-attachments/assets/3452599b-4336-45b9-9f7c-fc3139199c65" alt="app screenshot" width="300" />

## What is it?

An Android app to show the current and forecasted [wet bulb temperatures](https://en.wikipedia.org/wiki/Wet-bulb_temperature) based on your current location.

It also gives a brief description of how you ought to behave given the dangers posed by the current temperature based on [advice from the Japanese Ministry of the Enviroment](https://www.wbgt.env.go.jp/en/wbgt.php).

## Where does the weather data come from?

I'm using the [OpenMeteo API](https://open-meteo.com/), which is free for non-commercial use, fast, and has data for all the locations I cared to try.

## How is wet-bulb temperature calculated?

The formula used [is described in this publication](https://journals.ametsoc.org/view/journals/apme/50/11/jamc-d-11-0143.1.xml).

There are limitations, such as only being valid between -20 and +50 celcius and 5-99% humidity, and not taking wind speed into account. But I couldn't find a better one.

## Aren't there enough weather apps already?

Most don't display wet bulb temperature, which is increasingly relevant as global warming continues.

Those few apps that do display it, are either bloatware, iOS-only, require login, or are monetized.
This app is simple, free, runs on 95% of Android devices and doesn't track users or require a login.

Besides, I was bored on a weekend where, not surprisingly, it was too hot to go outside.

## Roadmap

Some nice features might be:

* notifications if temperatures are expected to cross into dangerous thresholds
* multi-day forecasts
* a widget that could be put on the home screen
* save and page through multiple locations
* a UI that isn't awful
