# Third-party acknowledgments

The Douyin Web detail request signature and URI-only live photo endpoint handling in this app were adapted from the implementation and documentation of [ucmao/media-parser](https://github.com/ucmao/media-parser), copyright (c) 2025–2026 ucmao, under the MIT License.

MIT License

Copyright (c) 2025-2026 ucmao

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## 0.4.0 protocol research

The independently written desktop page parser was informed by the data-format approach described in [mubaiqq/dyjx](https://github.com/mubaiqq/dyjx) (`extract_live_photos.py`): desktop note pages may embed per-image motion URLs in `self.__pace_f` records (`video.playAddr[].src`). No source code or assets from that project are included. [Filan616/douyin-livephoto-extractor](https://github.com/Filan616/douyin-livephoto-extractor) also documents that Live Photo motion is a separate video resource.
