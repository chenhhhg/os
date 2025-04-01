# 内存管理系统

## 概述

本项目实现了一个基于页式管理的虚拟内存系统，支持内存分配、释放、页面错误处理和内存保护功能。系统使用LRU（最近最少使用）算法进行页面置换。

## 主要组件

1. **页表项 (PageEntry)**：
   - 记录页面是否有效、是否被修改以及对应的物理页号。

2. **页面置换 (SwappedOutPage)**：
   - 保存被换出的页面信息，包括进程ID、虚拟页号和页面内容。

3. **内存管理接口 (IMemoryManager)**：
   - 定义内存管理单元的核心功能，如读写内存、页面重入、地址转换等。

4. **内存管理器 (MemoryManager)**：
   - 实现内存管理接口，提供具体的内存管理功能。
   - 使用单例模式，保证全局唯一实例。

5. **页面故障处理器 (PageFaultHandler)**：
   - 负责处理页面错误，通过页面重入机制将数据加载到内存。

## 内存管理流程

1. **内存初始化**：
   - 系统启动时，初始化物理内存、页面位图和最后访问时间数组。

2. **内存访问**：
   - 当进程需要访问内存时，首先通过内存管理器进行地址转换。
   - 如果页面不在内存中或无效，触发页面错误处理机制。

3. **页面错误处理**：
   - 将进程状态设置为等待状态。
   - 记录引起页面错误的虚拟地址。
   - 通过页面故障处理器，将所需页面加载到内存。

4. **页面置换**：
   - 当物理内存不足时，使用LRU算法选择最近最少使用的页面换出。
   - 被换出的页面保存在SwappedOutPage列表中，以便后续访问。

5. **资源清理**：
   - 进程终止时，通过MemoryManager的clearPageTable方法释放该进程占用的所有页面和页表资源。

## 系统集成

内存管理系统与CPU、进程管理等组件集成：

1. **CPU集成**：
   - CPU执行指令时，通过内存管理器进行内存读写操作。
   - 当遇到页面错误时，CPU暂停执行当前进程，等待页面重入完成。

2. **进程管理集成**：
   - PCB中增加记录页面错误虚拟地址的字段，以支持页面错误处理。
   - 进程调度器在页面错误后重新调度进程。

## 性能优化

1. **LRU算法**：使用访问时间记录页面最近使用情况，实现高效的页面置换策略。
2. **页面缓存**：通过保存换出页面内容，减少I/O操作，提高系统性能。

## 使用方法

要使用内存管理系统，需要以下步骤：

1. 初始化内存管理器：
```java
ArrayList<PCB> processes = ...; // 进程列表
Object[] memory = new Object[MemoryConstant.PHYSICAL_MEMORY_SIZE]; // 物理内存
MemoryManager.getInstance().initialize(processes, memory, MemoryConstant.PAGE_SIZE, MemoryConstant.VIRTUAL_ADDRESS_SIZE);
```

2. 初始化页面故障处理器：
```java
PageFaultHandler.initialize(MemoryManager.getInstance());
```

3. 使用内存管理器进行内存操作：
```java
// 读取内存
Object data = MemoryManager.getInstance().memoryRead(process, virtualAddress, cpuTick);

// 写入内存
boolean success = MemoryManager.getInstance().memoryWrite(process, virtualAddress, content, cpuTick);
```

4. 处理页面错误：
```java
if (process.getState() == ProcessState.WAITING) {
    PageFaultHandler.handlePageFault(process, process.getFaultVirtualAddress());
}
``` 